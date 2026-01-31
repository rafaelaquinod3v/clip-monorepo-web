package sv.com.clip.learning.domain.commands

import sv.com.clip.learning.domain.WordStatus
import java.util.UUID

data class UpdateUserWordStatusCommand(
  val userId: UUID,
  val term: String,
  val status: WordStatus
)
