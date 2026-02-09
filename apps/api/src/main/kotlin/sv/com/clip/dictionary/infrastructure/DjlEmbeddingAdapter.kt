package sv.com.clip.dictionary.infrastructure

import ai.djl.huggingface.translator.TextEmbeddingTranslator
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import jakarta.annotation.PreDestroy
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import sv.com.clip.dictionary.domain.EmbeddingProvider
import java.nio.file.Path
import java.nio.file.Paths

@Service
class DjlEmbeddingAdapter : EmbeddingProvider {
/*  private val model: ZooModel<String, FloatArray> = Criteria.builder()
    .setTypes(String::class.java, FloatArray::class.java) // TODO: urgent
    .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
    .optEngine("PyTorch")
    .build()
    .loadModel()*/
/*val resource = ClassPathResource("models/all-MiniLM-L6-v2")
  val modelPath: Path = Paths.get(resource.uri)
  private val model: ZooModel<String, FloatArray> = Criteria.builder()
    .setTypes(String::class.java, FloatArray::class.java)
    .optModelUrls("file://${modelPath.toAbsolutePath()}/")
    // Cambia optModelUrls por optModelPath apuntando a tu carpeta local
    //.optModelPath(modelPath)
    //.optModelName("pytorch_model.bin")
    //.optEngine("PyTorch")
    // El translator es necesario para que el modelo entienda el input String
    //.optTranslator(TextEmbeddingTranslator.builder().build())
    .optTranslatorFactory(TextEmbeddingTranslatorFactory())
    .optOption("trainable", "false")
    .build()
    .loadModel()*/

  override fun calculate(term: String): FloatArray {
    return floatArrayOf()
/*    model.newPredictor().use { predictor ->
      return predictor.predict(term)
    }*/
  }

/*  @PreDestroy
  fun close() = model.close()*/
}
