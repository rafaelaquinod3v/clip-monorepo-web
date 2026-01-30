package sv.com.clip.learning.application.mapper

import sv.com.clip.dictionary.api.WordTranslationDTO
import sv.com.clip.learning.application.WordAnalysis
import sv.com.clip.learning.domain.WordStatus

// In your Learning module mappers
fun List<WordTranslationDTO>.toAnalysis(status: WordStatus): WordAnalysis {
  // We assume all entries in the list are for the same word (e.g., 'bank')
  val firstEntry = this.first()

  return WordAnalysis(
    word = firstEntry.sourceLemma,
    // Join all definitions from different senses into one string
    definition = this.joinToString("; ") { it.targetLemma!! },
    status = status
  )
}
