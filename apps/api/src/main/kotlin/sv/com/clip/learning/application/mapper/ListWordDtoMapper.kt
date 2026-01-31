package sv.com.clip.learning.application.mapper

import sv.com.clip.dictionary.api.WordTranslationDTO
import sv.com.clip.learning.application.WordAnalysis
import sv.com.clip.learning.domain.WordStatus

fun List<WordTranslationDTO>.toAnalysis(term: String, lemma: String, status: WordStatus): WordAnalysis {

  return WordAnalysis(
    term = term,
    // Join all definitions from different senses into one string
    lemma = lemma,
    status = status,
    targetLemma = this.joinToString("; ") { it.targetLemma!! },
  )
}
