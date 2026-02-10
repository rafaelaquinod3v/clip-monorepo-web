package sv.com.clip.speech.internal

import com.k2fsa.sherpa.onnx.*

import org.springframework.core.io.ClassPathResource
import org.springframework.http.client.JdkClientHttpRequestFactory

import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body


import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.encode.AudioAttributes
import ws.schild.jave.encode.EncodingAttributes
import java.io.File
import java.net.http.HttpClient
import java.time.Duration

@Service
class TtsService(
  private val recognizerService: RecognizerService,
  private val phonemeService: PhonemeService,
  private val kokoroOnnxService: KokoroOnnxService,
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


/*
  private val restClient = RestClient.create("http://localhost:8880/v1").responseTimeout(Duration.ofSeconds(30)) // Dale tiempo a Kokoro
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
*/

  fun generateSpeech(text: String, voice: String = "af_heart"): ByteArray? {
/*    val nettyClient = HttpClient.create()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
      .responseTimeout(Duration.ofSeconds(30)) // Now it should appear with this import

    val webClient = WebClient.builder()
      .clientConnector(ReactorClientHttpConnector(nettyClient))
      .baseUrl("http://localhost:8880/v1")
      .build()*/
    val nativeClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10)) // Aquí se pone el CONNECT timeout
      .build()
    val factory = JdkClientHttpRequestFactory(nativeClient)
    factory.setReadTimeout(Duration.ofSeconds(30)) // This is the Response Timeout
   // factory.setConnectTimeout(Duration.ofSeconds(10))

    val restClient = RestClient.builder()
      .requestFactory(factory)
      .baseUrl("http://localhost:8880/v1")
      .build()

    val requestBody = mapOf(
      "input" to text,
      "voice" to voice,
      "model" to "tts-1", // Requerido por compatibilidad OpenAI
      //"response_format" to "mp3"
      "response_format" to "wav"
    )

    return restClient.post()
      .uri("/audio/speech")
      .body(requestBody)
      .retrieve()
      .body<ByteArray>() // Recibes los bytes del audio
  }

  fun generateAudioWithSyncV2(text: String): Map<String, Any> {
    //val audio = tts.generate(text)
    val samples = generateSpeech(text) //audio.samples
    val sampleRate = 24000 ///audio.sampleRate

    // 1. Padding: Add silence at the BEGINNING
    // val paddingSize = 8000
    //val paddedSamples = FloatArray(samples.size + paddingSize)
    //samples.copyInto(paddedSamples, paddingSize) // Move audio to start after 8000 samples
   //
    // 2. Recognize (Pass 22050 so Sherpa calculates seconds correctly)
    val floatArrayData = byteToFloatArrayWav(samples!!)
    val totalDuration = floatArrayData.size.toDouble() / sampleRate
    val result = recognizerService.getTimestampsFromAudio(floatArrayData, sampleRate.toFloat())
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
      "audio" to Base64.getEncoder().encodeToString(convertWavToMp3(samples)),
      "alignment" to wordAlignments
    )
  }



  fun byteToFloatArray(audioBytes: ByteArray): FloatArray {
    // 2 bytes por muestra en PCM 16-bit
    val n = audioBytes.size / 2
    val floatAudio = FloatArray(n)

    // Usamos ByteBuffer para manejar el orden de los bytes (Little Endian para Kokoro)
    val buffer = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN)

    for (i in 0 until n) {
      // Leemos el Short (16 bits) y normalizamos al rango [-1.0, 1.0]
      val pcmSample = buffer.short.toFloat()
      floatAudio[i] = pcmSample / 32768.0f
    }

    return floatAudio
  }

  fun byteToFloatArrayWav(audioBytes: ByteArray): FloatArray {
    val headerSize = 44 // Cabecera estándar de WAV
    val pcmBytes = audioBytes.copyOfRange(headerSize, audioBytes.size)

    val n = pcmBytes.size / 2
    val floatAudio = FloatArray(n)
    val buffer = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)

    for (i in 0 until n) {
      floatAudio[i] = buffer.short.toFloat() / 32768.0f
    }
    return floatAudio
  }

  fun generateAudioWithSyncV3(text: String): Map<String, Any> {
    //val audio = tts.generate(text)
    val ipaPhonemes = phonemeService.getPhonemes(text)
    val samples = kokoroOnnxService.generateAudio(ipaPhonemes) //audio.samples
    val sampleRate = 24000 ///audio.sampleRate

    // 1. Padding: Add silence at the BEGINNING
    // val paddingSize = 8000
    //val paddedSamples = FloatArray(samples.size + paddingSize)
    //samples.copyInto(paddedSamples, paddingSize) // Move audio to start after 8000 samples
    //
    // 2. Recognize (Pass 22050 so Sherpa calculates seconds correctly)
    val wav = convertSamplesToWav(samples, sampleRate.toFloat())
    val floatArrayData = byteToFloatArrayWav(wav)
    val totalDuration = floatArrayData.size.toDouble() / sampleRate
    val result = recognizerService.getTimestampsFromAudio(floatArrayData, sampleRate.toFloat())
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
      "audio" to Base64.getEncoder().encodeToString(convertWavToMp3(wav)),
      "alignment" to wordAlignments
    )
  }
}

