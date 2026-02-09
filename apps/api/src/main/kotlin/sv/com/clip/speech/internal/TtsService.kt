package sv.com.clip.speech.internal

import com.k2fsa.sherpa.onnx.*
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.encode.AudioAttributes
import ws.schild.jave.encode.EncodingAttributes
import java.io.File

@Service
class TtsService(
  private val recognizerService: RecognizerService,
) {

  private val tts: OfflineTts
  // Sample Rate nativo fijo de 22050 Hz
  //private val sampleRate = 22050.0f // en_US-amy-low.onnx

  init {
    val modelResource = ClassPathResource("models/en_US-amy-low.onnx").file.absolutePath
    val tokensResource = ClassPathResource("models/tokens.txt").file.absolutePath
    val dataDirResource = ClassPathResource("models/espeak-ng-data").file.absolutePath
    // 1. Build VITS Config
    val vitsConfig = OfflineTtsVitsModelConfig.builder()
      .setModel(modelResource)
      .setTokens(tokensResource)
      .setDataDir(dataDirResource)
      .setNoiseScale(0.667f)
      .setNoiseScaleW(0.8f)
      .setLengthScale(1f)
      .build()

    // 2. Build Model Config (Includes VITS and threading)
    val modelConfig = OfflineTtsModelConfig.builder()
      .setVits(vitsConfig)
      .setNumThreads(4)
      .setDebug(false)
      .setProvider("cpu")
      .build()

    // 3. Build Main TTS Config
    val mainConfig = OfflineTtsConfig.builder()
      .setModel(modelConfig)
      .build()

    // 4. Instantiate the engine
    tts = OfflineTts(mainConfig)
  }

  fun generateWav(text: String): ByteArray {
    val audio = tts.generate(text)

    val samples = audio.samples // This is a FloatArray

    if (samples.isEmpty()) return ByteArray(0)

    return convertSamplesToWav(samples, audio.sampleRate.toFloat())
  }

  private fun writeWavHeader(out: ByteArrayOutputStream, numSamples: Int, sampleRate: Float) {
    val channels = 1
    val byteRate = sampleRate * channels * 2
    val blockAlign = channels * 2
    val dataSize = numSamples * channels * 2
    val chunkSize = 36 + dataSize

    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray())
    header.putInt(chunkSize)
    header.put("WAVE".toByteArray())
    header.put("fmt ".toByteArray())
    header.putInt(16) // Subchunk1Size
    header.putShort(1.toShort()) // AudioFormat (PCM)
    header.putShort(channels.toShort())
    header.putInt(sampleRate.toInt())
    header.putInt(byteRate.toInt())
    header.putShort(blockAlign.toShort())
    header.putShort(16.toShort()) // BitsPerSample
    header.put("data".toByteArray())
    header.putInt(dataSize)

    out.write(header.array())
  }
  fun generateAudioWithSync(text: String): Map<String, Any> {
    val audio = tts.generate(text)
    val samples = audio.samples
    val sampleRate = audio.sampleRate

    // 1. Padding: Add silence at the BEGINNING
   // val paddingSize = 8000
    //val paddedSamples = FloatArray(samples.size + paddingSize)
    //samples.copyInto(paddedSamples, paddingSize) // Move audio to start after 8000 samples
    val totalDuration = samples.size.toDouble() / sampleRate
    // 2. Recognize (Pass 22050 so Sherpa calculates seconds correctly)
    val result = recognizerService.getTimestampsFromAudio(samples, sampleRate.toFloat())
    // 3. Group sub-tokens into Words
    val wordAlignments = mutableListOf<Map<String, Any>>()
   // var currentWord: MutableMap<String, Any>? = null
    var currentWord: MutableMap<String, Any>? = null
    var wordCounter = 0 // This corresponds to the index in a split(" ") array

    result.tokens.forEachIndexed { i, token ->
      val punctuationRegex = Regex("[.,!?;:]")
      // Whisper marca el inicio de palabra con " " (espacio de BPE)
      val isNewWord = token.startsWith(" ") || token.startsWith("_")
      //val cleanToken = token.replace(" ", "").replace("_", "")
      val hasSpace = token.startsWith(" ") || token.startsWith("_")
      val cleanToken = token.replace(Regex("[ _]"), "")
      val isContinuation = !hasSpace || cleanToken.matches(punctuationRegex)
      //val cleanToken = token.replace(Regex("[ _]"), "")
      val nextTokenStart = result.timestamps.getOrElse(i + 1) { totalDuration.toFloat() }
      if (!isContinuation || currentWord == null) {
        // If it's a new word, save the previous one and start a new one
        currentWord?.let {
          wordAlignments.add(it)
          wordCounter += 2 // frontend text.split(/(\s+)/)
        }

        currentWord = mutableMapOf(
          "term" to cleanToken,
          "start" to result.timestamps[i],
          "end" to nextTokenStart,
          "index" to wordCounter
        )
      } else {
        // It's a sub-token (like "ll" or "o"), append to the current word
        currentWord["term"] = currentWord["term"].toString() + cleanToken
        currentWord["end"] = nextTokenStart
      }
    }
    // Add the last word
    currentWord?.let { wordAlignments.add(it) }

    //wordAlignments.forEach { println(it["term"]) }

    return mapOf(
      "sampleRate" to sampleRate,
      "duration" to totalDuration,
      "audio" to Base64.getEncoder().encodeToString(convertWavToMp3(convertSamplesToWav(samples, sampleRate.toFloat()))),
      "alignment" to wordAlignments
    )
  }

  // Refactor your existing generateWav logic into a helper that takes samples
  private fun convertSamplesToWav(samples: FloatArray, sampleRate: Float): ByteArray {
    val out = ByteArrayOutputStream()
    writeWavHeader(out, samples.size, sampleRate)
    val buffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
    for (sample in samples) {
      val s = (sample * 32767).toInt().coerceIn(-32768, 32767)
      buffer.putShort(s.toShort())
    }
    out.write(buffer.array())
    return out.toByteArray()
  }

  fun convertWavToMp3(wavBytes: ByteArray): ByteArray {
    // JAVE2 a veces requiere un archivo temporal porque FFmpeg lo prefiere
    val tempWav = File.createTempFile("temp_audio", ".wav")
    val tempMp3 = File.createTempFile("temp_audio", ".mp3")

    try {
      tempWav.writeBytes(wavBytes)

      // Configuración de Audio
      val audio = AudioAttributes().apply {
        setCodec("libmp3lame")
        setBitRate(64000) // 64kbps es ideal para voz
        setChannels(1)
        setSamplingRate(16000) // Mantener tus 16k
      }

      // Configuración de Codificación
      val attrs = EncodingAttributes().apply {
        setOutputFormat("mp3")
        setAudioAttributes(audio)
      }

      // Ejecutar conversión
      Encoder().encode(MultimediaObject(tempWav), tempMp3, attrs)

      return tempMp3.readBytes()
    } finally {
      // Limpieza de archivos temporales
      tempWav.delete()
      tempMp3.delete()
    }
  }
}

