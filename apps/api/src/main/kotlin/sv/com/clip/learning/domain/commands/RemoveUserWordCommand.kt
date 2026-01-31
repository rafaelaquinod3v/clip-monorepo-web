package sv.com.clip.learning.domain.commands

import java.util.UUID

data class RemoveUserWordCommand(
  val userId: UUID,
  val term: String,
)
