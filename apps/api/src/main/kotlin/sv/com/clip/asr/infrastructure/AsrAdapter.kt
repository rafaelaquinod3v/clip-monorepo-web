package sv.com.clip.asr.infrastructure

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizerResult
import jakarta.annotation.PostConstruct
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import sv.com.clip.asr.domain.AsrPort

@Component
class AsrAdapter : AsrPort {
  private lateinit var recognizer: OfflineRecognizer
  @PostConstruct
  fun init() {
    val encoder = ClassPathResource("models/sherpa-onnx-nemo-parakeet_tdt_ctc/model.int8.onnx").file.absolutePath
    val tokens = ClassPathResource("models/sherpa-onnx-nemo-parakeet_tdt_ctc/tokens.txt").file.absolutePath

    // 1. Para este modelo específico usa 80
    val featConfig = FeatureConfig.Builder()
      .setSampleRate(24000) // kokoro tts
      .setFeatureDim(80)
      .build()

    // 2. Configuración CTC (Evita el error de dimensiones del Joiner)
    val ctcConfig = OfflineNemoEncDecCtcModelConfig.Builder()
      .setModel(encoder)
      .build()

    val modelConfig = OfflineModelConfig.Builder()
      .setNemo(ctcConfig)
      .setTokens(tokens)
      .setModelType("nemo_ctc") // Importante para modelos 2023+
      .setNumThreads(1)
      .build()

    val config = OfflineRecognizerConfig.Builder()
      .setOfflineModelConfig(modelConfig)
      .setFeatureConfig(featConfig)
      .setDecodingMethod("greedy_search")
      .build()

    recognizer = OfflineRecognizer(config)
  }

  override fun getTimestampsFromAudio(samples: FloatArray, sampleRate: Float): OfflineRecognizerResult {
    val stream = recognizer.createStream()
    if (samples.isEmpty()) {
      println("Received empty audio for timestamping.")
    }

    stream.acceptWaveform(samples, sampleRate.toInt())

    recognizer.decode(stream)
    val result = recognizer.getResult(stream)

    println("Whisper detected text: ${result.text}") // <--- Check this!
    println("Whisper detected tokens: ${result.tokens.joinToString()}")
    println("Timestamps size: ${result.timestamps.size}")
    // Ensure native resources are released
    stream.release()
    result.tokens.zip(result.timestamps.toList()).take(30).forEach { (tok, ts) ->
      println("TOKEN: '${tok}' (bytes: ${tok.toByteArray().joinToString(",")}) @ $ts")
    }
    return result
  }

  // usage example alignment
  /*  fun generateAudioWithSync(text: String): Map<String, Any> {

      val samples = generateSpeech(text) //audio.samples
      val sampleRate = 24000 ///audio.sampleRate

      // 2. Recognize (Pass 22050 so Sherpa calculates seconds correctly)
      val floatArrayData = audioService.byteToFloatArrayWav(samples!!)
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

      return mapOf(
        "sampleRate" to sampleRate,
        "duration" to totalDuration,
        "audio" to Base64.getEncoder().encodeToString(audioService.convertWavToMp3(samples)),
        "alignment" to wordAlignments
      )
    }*/
}
