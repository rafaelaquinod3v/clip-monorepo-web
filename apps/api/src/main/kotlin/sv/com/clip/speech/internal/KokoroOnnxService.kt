package sv.com.clip.speech.internal

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.stereotype.Service
import java.nio.LongBuffer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

// Representa la parte interna del JSON: "model": { "vocab": { ... } }
@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenizerConfig(val model: ModelConfig)
data class ModelConfig(val vocab: Map<String, Long>)

@Service
class KokoroOnnxService(private val voiceStyleService: VoiceStyleService) {
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
    // 1. Tokenización: Convertir String de fonemas a IDs numéricos
    //val tokens = phonemes.map { phonemeIds[it] ?: 0 }.map { it.toLong() }.toLongArray()
    val tokens = textToTokenIds(phonemes)
    val styleArray = voiceStyleService.loadStyle("af_heart")
    // Agregar tokens de inicio/fin si el modelo lo requiere (ej: [0] al inicio)
    val inputIds = tokens // longArrayOf(0) + tokens + longArrayOf(0)

    // 2. Crear Tensores de entrada
    val container = mapOf(
      "input_ids" to OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), longArrayOf(1, inputIds.size.toLong())),
      "style_id" to OnnxTensor.createTensor(env, java.nio.FloatBuffer.wrap(styleArray), longArrayOf(1, 256)),
      "speed" to OnnxTensor.createTensor(env, floatArrayOf(1.0f))
    )

    // 3. Ejecutar Inferencia
    val result = session.run(container)

    // 4. Extraer el FloatArray (Audio)
    val outputTensor = result[0].value as Array<FloatArray>
    return outputTensor[0] // Retorna el audio crudo (PCM)
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
