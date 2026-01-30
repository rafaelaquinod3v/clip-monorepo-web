package sv.com.clip.learning.application

import sv.com.clip.learning.domain.WordStatus

data class WordAnalysis(
  val term: String,        // El término original
  val lemma: String? = null,  // la raíz de la palabra para mejor clasificación
  val status: WordStatus,  // (KNOWN, LEARNING, NEW)
  val targetLemma: String? = null,
)
