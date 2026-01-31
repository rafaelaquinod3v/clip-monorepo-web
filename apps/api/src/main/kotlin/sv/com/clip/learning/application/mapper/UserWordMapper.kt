package sv.com.clip.learning.application.mapper

import sv.com.clip.learning.application.WordAnalysis
import sv.com.clip.learning.domain.UserWord

fun UserWord.toAnalysis(term: String): WordAnalysis {
  return WordAnalysis(
    term = term,
    lemma = this.lemma,
    status = this.status,
    targetLemma = this.targetLemma ?: "",
  )
}
