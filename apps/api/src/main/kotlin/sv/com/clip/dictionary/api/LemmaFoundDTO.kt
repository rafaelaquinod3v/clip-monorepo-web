package sv.com.clip.dictionary.api

import java.util.UUID

data class LemmaFoundDTO(
  val id: UUID,
  val lemma: String,
)
