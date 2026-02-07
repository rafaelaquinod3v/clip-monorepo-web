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
  private val sampleRate = 22050.0f

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
    val sampleRate = audio.sampleRate // 22050

    // 1. Padding: Add silence at the BEGINNING
    //val paddingSize = 8000
    //val paddedSamples = FloatArray(samples.size + paddingSize)
    //samples.copyInto(paddedSamples, paddingSize) // Move audio to start after 8000 samples

    // 2. Recognize (Pass 22050 so Sherpa calculates seconds correctly)
    //val result = recognizerService.getTimestampsFromAudio(paddedSamples, 22050f)
    val result = recognizerService.getTimestampsFromAudio(samples, 22050f)
    // 3. Group sub-tokens into Words
    val wordAlignments = mutableListOf<Map<String, Any>>()
    var currentWord: MutableMap<String, Any>? = null
    var wordIndex = 0

    result.tokens.forEachIndexed { i, token ->
      // Zipformer tokens starting with " " (or "_" in some models) mark a NEW word
      val isNewWord = token.startsWith(" ") || token.startsWith("_")
      val cleanToken = token.replace(" ", "").replace("_", "")

      if (isNewWord || currentWord == null) {
        // If it's a new word, save the previous one and start a new one
        currentWord?.let { wordAlignments.add(it) }

        currentWord = mutableMapOf(
          "term" to cleanToken,
          "start" to result.timestamps[i],
          "end" to result.timestamps[i] + 0.1,
          "originalIndex" to wordIndex++
        )
      } else {
        // It's a sub-token (like "ll" or "o"), append to the current word
        currentWord["term"] = currentWord["term"].toString() + cleanToken
        currentWord["end"] = result.timestamps[i] + 0.1
      }
    }
    // Add the last word
    currentWord?.let { wordAlignments.add(it) }

    return mapOf(
      "audio" to Base64.getEncoder().encodeToString(convertSamplesToWav(samples, sampleRate.toFloat())),
      "alignment" to wordAlignments
    )
  }

/*  fun generateAudioWithSync(text: String): Map<String, Any> {
    if (text.isBlank()) {
      return mapOf("audio" to "", "alignment" to emptyList<Any>())
    }
    // 1. GENERATE AUDIO ONCE
    val audio = tts.generate(text)
    val samples = audio?.samples ?: floatArrayOf()
    val sampleRate = audio?.sampleRate?.toFloat() ?: 22050.0f
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
    val paddingStart = 8000.0 / sampleRate  // Exactly 0.3628s
    val paddingEnd = 400.0 / sampleRate     // Exactly 0.0181s
    val speakingDuration = totalDuration - paddingStart - paddingEnd

    val alignment = if (timestamps.isNotEmpty()) {
      println("timestamps: ${timestamps.size}")
      // Use real timestamps if available
      tokens.mapIndexed { i, token ->
        mapOf(
          "term" to token,
          "start" to recognitionResult.timestamps[i],
          "end" to recognitionResult.timestamps.getOrElse(i + 1) { (recognitionResult.timestamps[i] + 0.1).toFloat() },
          "originalIndex" to i
        )
      }
    } else {
      // FALLBACK: Distribute tokens found by Whisper linearly across total duration
      val totalChars = tokens.sumOf { it.length }.toDouble()
      var currentTime = paddingStart

      tokens.mapIndexed { i , token ->
        // Duration proportional to word length relative to total audio length
        val wordDuration = (token.length / totalChars) * speakingDuration
        val start = currentTime
        val end = currentTime + wordDuration
        currentTime = end

        mapOf(
          "term" to token,
          "start" to start,
          "end" to end,
          "originalIndex" to i
        )
      }
    }

   // val wavBytes = convertSamplesToWav(samples, sampleRate)

    return mapOf(
      "audio" to audioBase64,
      "alignment" to alignment
    )
  }*/

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
}

