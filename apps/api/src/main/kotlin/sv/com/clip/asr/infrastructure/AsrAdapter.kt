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
}
