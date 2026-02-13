package sv.com.clip.text.internal

import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.sentdetect.SentenceModel
import org.springframework.stereotype.Service
import sv.com.clip.text.api.TextProcessorExternal
import java.io.FileNotFoundException

@Service
class TextProcessorService : TextProcessorExternal {
  private val resourcePath = "models/opennpl/en-sent.bin"
  private val modelIn = object {}.javaClass.classLoader.getResourceAsStream(resourcePath)
    ?: throw FileNotFoundException("Modelo no encontrado en resources: $resourcePath")
  private val model = SentenceModel(modelIn)
  private val sentenceDetector = SentenceDetectorME(model)

  override fun splitBySentence(text: String): List<String> {
    // Get Spans (Safest way to keep EVERY character)
    val spans = sentenceDetector.sentPosDetect(text)
    return spans.map { span ->
      text.substring(span.start, span.end)
    }
  }
}
