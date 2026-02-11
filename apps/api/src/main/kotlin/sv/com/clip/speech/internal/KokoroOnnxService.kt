package sv.com.clip.speech.internal

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.stereotype.Service
import java.nio.LongBuffer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.math.abs

// Representa la parte interna del JSON: "model": { "vocab": { ... } }
@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenizerConfig(val model: ModelConfig)
data class ModelConfig(val vocab: Map<String, Long>)

@Service
class KokoroOnnxService(private val voiceStyleService: VoiceStyleService, private val phonemeService: PhonemeService) {
  private val env = OrtEnvironment.getEnvironment()
  private val session: OrtSession
  private val vocab: Map<String, Long>

  init {
    // Carga el modelo desde los recursos o una ruta fija
    val modelBytes = this::class.java.classLoader.getResourceAsStream("models/kokoro-82M-v1_0-ONNX/model.onnx")?.readAllBytes()
      ?: throw RuntimeException("Modelo no encontrado")
    println("KokoroOnnxService initiated")
    session = env.createSession(modelBytes)

    // Cargar el archivo tokenizer.json desde resources
    val jsonStream = this::class.java.classLoader.getResourceAsStream("models/kokoro-82M-v1_0-ONNX/tokenizer.json")
      ?: throw RuntimeException("No se encontró tokenizer.json")

    val config: TokenizerConfig = jacksonObjectMapper().readValue(jsonStream)
    vocab = config.model.vocab
  }


  // Mapeo simple de caracteres IPA a IDs (Simplificado para el ejemplo)
  // Nota: Kokoro tiene un vocabulario específico de ~178 tokens fonéticos
  //private val phonemeIds = mapOf('h' to 75, 'e' to 72, 'l' to 79, 'o' to 82 /* ... completar mapeo ... */) tokenizer.json

  fun generateAudio(phonemes: String): FloatArray {
    println("generateAudio fun")
    // 1. Tokenización: Convertir String de fonemas a IDs numéricos
    //val tokens = phonemes.map { phonemeIds[it] ?: 0 }.map { it.toLong() }.toLongArray()
    val tokens = textToTokenIds(phonemes)
    //val styleArray = voiceStyleService.loadStyle("af_heart")

    // 1. Get style (ensure "af_heart" is loaded correctly)
    val styleArray = voiceStyleService.loadStyle("af_heart")

    // 2. Adjust Speed & Language
    // 1.0f can sometimes feel rushed in Kokoro. Try 0.9f or 0.85f for a more natural pace.
    val speedValue = 0.8f

    // Kokoro usually expects a 'language' tensor (int64) for multi-lang models
    // 0 = American English, 1 = British English (check your model version)
    val langIds = longArrayOf(0)
   // val inputIds =
    // Agregar tokens de inicio/fin si el modelo lo requiere (ej: [0] al inicio)


    // 2. Crear Tensores de entrada
    val container = mapOf(
      "input_ids" to OnnxTensor.createTensor(env, LongBuffer.wrap(tokens), longArrayOf(1, tokens.size.toLong())),
      "style" to OnnxTensor.createTensor(env, java.nio.FloatBuffer.wrap(styleArray), longArrayOf(1, 256)),
      "speed" to OnnxTensor.createTensor(env, floatArrayOf(speedValue)),

    )

/*    // 3. Ejecutar Inferencia
    val result = session.run(container)

    // 4. Extraer el FloatArray (Audio)
    val outputTensor = result[0].value as Array<FloatArray>
    return outputTensor[0] // Retorna el audio crudo (PCM)*/
    val result = session.run(container)

    // Fixed extraction logic for Kokoro
    val outputValue = result[0].value
    val audio = if (outputValue is Array<*> && outputValue[0] is FloatArray) {
      outputValue[0] as FloatArray
    } else outputValue as? FloatArray
        ?: throw IllegalStateException("Unexpected output type from ONNX: ${outputValue::class.java}")

    result.close() // Important: Close result to prevent memory leaks

    // Final Step: Normalize to remove the 'whisper' (low volume) effect
    return normalizeAudio(audio)
  }
  private fun normalizeAudio(audio: FloatArray): FloatArray {
    val max = audio.maxOfOrNull { abs(it) } ?: 1f
    if (max < 0.01f) return audio
    // Boost volume to 90% of max range
    return FloatArray(audio.size) { i -> (audio[i] / max) * 0.9f }
  }
  fun textToTokenIds(phonemes: String): LongArray {
    val tokens = mutableListOf<Long>()

    // El modelo espera que la secuencia empiece con ID 0 ($)
    tokens.add(0L)

    for (char in phonemes) {
      val id = vocab[char.toString()]
      if (id != null) {
        tokens.add(id)
      }
    }

    // El modelo espera que la secuencia termine con ID 0 ($)
    tokens.add(0L)

    return tokens.toLongArray()
  }
}
