package sv.com.clip.speech.internal

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizerResult
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import jakarta.annotation.PostConstruct
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class RecognizerService {
  private lateinit var recognizer: OfflineRecognizer

  @PostConstruct
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


  fun getTimestampsFromAudio(samples: FloatArray, sampleRate: Int): OfflineRecognizerResult {
    val stream = recognizer.createStream()


    stream.acceptWaveform(samples, sampleRate)

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
