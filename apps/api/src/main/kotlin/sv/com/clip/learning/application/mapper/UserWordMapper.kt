package sv.com.clip.learning.application.mapper

import sv.com.clip.learning.application.WordAnalysis
import sv.com.clip.learning.domain.UserWord

fun UserWord.toAnalysis(): WordAnalysis {
  return WordAnalysis(
    word = this.lemma,
    // If it's a personal word, we use the custom definition
    definition = this.targetGloss ?: "No definition provided",
    status = this.status,
    // We can add a 'source' if your WordAnalysis DTO supports it
    // source = "PERSONAL"
  )
}
