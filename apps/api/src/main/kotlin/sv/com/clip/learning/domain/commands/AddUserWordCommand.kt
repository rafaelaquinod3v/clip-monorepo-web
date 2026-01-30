package sv.com.clip.learning.domain.commands

import sv.com.clip.learning.domain.WordStatus
import java.util.UUID

data class AddUserWordCommand(
  val userId: UUID,
  val lemma: String,
  val status: WordStatus,
)
