package sv.com.clip.speech.internal

import com.k2fsa.sherpa.onnx.*
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64

@Service
class TtsService(
  private val recognizerService: RecognizerService,
) {

  private val tts: OfflineTts
  private val sampleRate = 22050

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
      .setLengthScale(1.3f)
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
    return convertSamplesToWav(samples, sampleRate)
/*    val out = ByteArrayOutputStream()

    // 1. Write WAV Header (44 bytes)
    writeWavHeader(out, samples.size, sampleRate)

    // 2. Convert Float samples (-1.0 to 1.0) to PCM 16-bit (Short)
    val buffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
    for (sample in samples) {
      val s = (sample * 32767).toInt().coerceIn(-32768, 32767)
      buffer.putShort(s.toShort())
    }

    out.write(buffer.array())
    return out.toByteArray()*/
  }

  private fun writeWavHeader(out: ByteArrayOutputStream, numSamples: Int, sampleRate: Int) {
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
    header.putInt(sampleRate)
    header.putInt(byteRate)
    header.putShort(blockAlign.toShort())
    header.putShort(16.toShort()) // BitsPerSample
    header.put("data".toByteArray())
    header.putInt(dataSize)

    out.write(header.array())
  }

  fun generateAudioWithSync(text: String): Map<String, Any> {
    if (text.isBlank()) {
      return mapOf("audio" to "", "alignment" to emptyList<Any>())
    }
    // 1. GENERATE AUDIO ONCE
    val audio = tts.generate(text)
    val samples = audio?.samples ?: floatArrayOf()
    val sampleRate = audio?.sampleRate ?: 22050
    if (samples.isEmpty()) {
      return mapOf("audio" to "", "alignment" to emptyList<Any>())
    }
    // 2. GET TIMESTAMPS (using the already generated samples)
// Inside generateAudioWithSync
    val paddedSamples = FloatArray(samples.size + 8000) // Add ~0.3s of silence
    samples.copyInto(paddedSamples)

    val recognitionResult = recognizerService.getTimestampsFromAudio(paddedSamples, sampleRate)


    // 3. CONVERT SAMPLES TO WAV BYTES (reuse your existing logic)
    val wavBytes = convertSamplesToWav(samples, sampleRate)

    // 4. ENCODE TO BASE64
    val audioBase64 = Base64.getEncoder().encodeToString(wavBytes)

    // 6. PROTECT ALIGNMENT MAPPING
    // We check if tokens and timestamps actually exist and have the same size
    val totalDuration = samples.size.toDouble() / sampleRate
    val tokens = recognitionResult.tokens
    val timestamps = recognitionResult.timestamps

    val alignment = if (timestamps.isNotEmpty()) {
      // Use real timestamps if available
      tokens.mapIndexed { i, token ->
        mapOf(
          "word" to token,
          "start" to recognitionResult.timestamps[i],
          "end" to recognitionResult.timestamps.getOrElse(i + 1) { (recognitionResult.timestamps[i] + 0.1).toFloat() }
        )
      }
    } else {
      // FALLBACK: Distribute tokens found by Whisper linearly across total duration
      val totalChars = tokens.sumOf { it.length }.toDouble()
      var currentTime = 0.0

      tokens.map { token ->
        // Duration proportional to word length relative to total audio length
        val wordDuration = (token.length / totalChars) * totalDuration
        val start = currentTime
        val end = currentTime + wordDuration
        currentTime = end

        mapOf(
          "term" to token,
          "start" to start,
          "end" to end
        )
      }
    }

   // val wavBytes = convertSamplesToWav(samples, sampleRate)

    return mapOf(
      "audio" to audioBase64,
      "alignment" to alignment
    )
  }

  // Refactor your existing generateWav logic into a helper that takes samples
  private fun convertSamplesToWav(samples: FloatArray, sampleRate: Int): ByteArray {
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
}

