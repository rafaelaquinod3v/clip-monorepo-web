package sv.com.clip.speech.internal

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizerResult
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import jakarta.annotation.PostConstruct
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class RecognizerService {
  private lateinit var recognizer: OfflineRecognizer
  @PostConstruct
  fun init() {
    val encoder = ClassPathResource("models/sherpa-onnx-nemo-parakeet_tdt_ctc/model.int8.onnx").file.absolutePath
    val tokens = ClassPathResource("models/sherpa-onnx-nemo-parakeet_tdt_ctc/tokens.txt").file.absolutePath

    // 1. Para este modelo específico usa 80
    val featConfig = FeatureConfig.Builder()
      .setSampleRate(16000)
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

fun getTimestampsFromAudio(samples: FloatArray, sampleRate: Float): OfflineRecognizerResult {
  val stream = recognizer.createStream()


  stream.acceptWaveform(samples, sampleRate.toInt())

  recognizer.decode(stream)
  val result = recognizer.getResult(stream)
  println("Whisper detected text: ${result.text}") // <--- Check this!
  println("Whisper detected tokens: ${result.tokens.joinToString()}")
  println("Timestamps size: ${result.timestamps.size}")
  // Ensure native resources are released
  stream.release()

  return result
}
}
