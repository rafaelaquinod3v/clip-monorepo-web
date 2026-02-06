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

/*  @PostConstruct
  fun init() {
    // 1. Path setup (using ClassPathResource as in your TtsService)
    val encoder = ClassPathResource("models/sherpa-onnx-streaming-zipformer/encoder-epoch-99-avg-1-chunk-16-left-128.onnx").file.absolutePath
    val decoder = ClassPathResource("models/sherpa-onnx-streaming-zipformer/decoder-epoch-99-avg-1-chunk-16-left-128.onnx").file.absolutePath
    val joiner = ClassPathResource("models/sherpa-onnx-streaming-zipformer/joiner-epoch-99-avg-1-chunk-16-left-128.onnx").file.absolutePath
    val tokens = ClassPathResource("models/sherpa-onnx-streaming-zipformer/tokens.txt").file.absolutePath

    // 2. Transducer Config (Zipformer)
    val transducerConfig = OfflineTransducerModelConfig.Builder()
      .setEncoder(encoder)
      .setDecoder(decoder)
      .setJoiner(joiner)
      .build()

    // 3. Model Config
    val modelConfig = OfflineModelConfig.Builder()
      .setTransducer(transducerConfig)
      .setTokens(tokens)
      .setModelType("transducer")
      .setNumThreads(1)
      .build()

    // 4. Feature Config (Standard for Zipformer)
    val featConfig = FeatureConfig.Builder()
      .setSampleRate(16000)
      .setFeatureDim(45)
      .build()

    // 5. Main Recognizer Config
    val config = OfflineRecognizerConfig.Builder()
      .setOfflineModelConfig(modelConfig)
      .setFeatureConfig(featConfig)
      .setDecodingMethod("greedy_search")
      .build()

    recognizer = OfflineRecognizer(config)
  }*/

/*  fun getTimestampsFromAudio(samples: FloatArray, sampleRate: Int): OfflineRecognizerResult {
    val stream = recognizer.createStream()
    stream.acceptWaveform(samples, sampleRate)
    recognizer.decode(stream)
    val result = recognizer.getResult(stream)
    stream.release()
    return result
  }*/

/*  @PostConstruct
  fun init() {
    val encoderResource = ClassPathResource("models/sherpa-onnx-whisper-tiny.en/tiny.en-encoder.onnx").file.absolutePath
    val tokensResource = ClassPathResource("models/sherpa-onnx-whisper-tiny.en/tiny.en-tokens.txt").file.absolutePath
    val decoderResource = ClassPathResource("models/sherpa-onnx-whisper-tiny.en/tiny.en-decoder.onnx").file.absolutePath
    println("Cargando modelo desde: $decoderResource")
    val whisperConfig = OfflineWhisperModelConfig.Builder()
      .setEncoder(encoderResource)
      .setDecoder(decoderResource)
      .setLanguage("en") // Force English
      .setTask("transcribe")
      .setTailPaddings(400)
      .build()

    val modelConfig = OfflineModelConfig.Builder()
      .setWhisper(whisperConfig) // <--- El método vive AQUÍ
      .setTokens(tokensResource)
      .setModelType("whisper")
      .setNumThreads(1)
      .setDebug(false)
      .build()

    val featConfig = FeatureConfig.Builder()
      .setSampleRate(16000)
      .setFeatureDim(80)
      .build()

    val config = OfflineRecognizerConfig.Builder()
      .setOfflineModelConfig(modelConfig)
      .setFeatureConfig(featConfig)
      .setDecodingMethod("greedy_search")
      .build()

    recognizer = OfflineRecognizer(config)
  }


*/
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
