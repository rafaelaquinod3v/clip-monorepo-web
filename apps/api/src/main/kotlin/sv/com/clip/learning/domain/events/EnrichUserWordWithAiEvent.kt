package sv.com.clip.learning.domain.events

import java.util.UUID

data class EnrichUserWordWithAiEvent(
  val id: UUID,
  val term: String,
)
