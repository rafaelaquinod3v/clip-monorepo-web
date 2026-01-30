package sv.com.clip.dictionary.infrastructure.gateways

import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.springframework.stereotype.Service

@Service
class LemmatizerService {
  fun lemmatize(term: String): String {
    val analyzer = EnglishAnalyzer()
    val tokenStream = analyzer.tokenStream(null, term)
    val termAttr = tokenStream.addAttribute(CharTermAttribute::class.java)

    tokenStream.reset()
    val result = mutableListOf<String>()
    while (tokenStream.incrementToken()) {
      result.add(termAttr.toString())
    }
    tokenStream.end()
    tokenStream.close()

    // Retornamos la primera palabra lematizada (ej: "banks" -> "bank")
    return result.firstOrNull() ?: term.lowercase()
  }
}
