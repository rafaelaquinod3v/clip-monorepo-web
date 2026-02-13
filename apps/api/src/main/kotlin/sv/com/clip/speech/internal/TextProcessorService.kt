package sv.com.clip.speech.internal

import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.sentdetect.SentenceModel
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.io.FileNotFoundException

@Service
class TextProcessorService {
  private val resourcePath = "models/opennpl/en-sent.bin"
  private val modelIn = object {}.javaClass.classLoader.getResourceAsStream(resourcePath)
    ?: throw FileNotFoundException("Modelo no encontrado en resources: $resourcePath")
  private val model = SentenceModel(modelIn)
  private val sentenceDetector = SentenceDetectorME(model)

  fun splitSentences(text: String): Array<String> {
    // Busca el archivo dentro de src/main/resources/models/opennpl/en-sent.bin
    val resourcePath = "models/opennpl/en-sent.bin"
    val modelIn = object {}.javaClass.classLoader.getResourceAsStream(resourcePath)
      ?: throw FileNotFoundException("Modelo no encontrado en resources: $resourcePath")

    val model = SentenceModel(modelIn)
    val detector = SentenceDetectorME(model)

    return detector.sentDetect(text)
  }

  fun splitText(text: String): List<String> {
    // Option A: Get raw strings (usually keeps punctuation)
    // return sentenceDetector.sentDetect(text).toList()

    // Option B: Get Spans (Safest way to keep EVERY character)
    val spans = sentenceDetector.sentPosDetect(text)
    return spans.map { span ->
      text.substring(span.start, span.end)
    }
  }
}
