package sv.com.clip.learning.domain.commands

import java.util.UUID

data class RemoveUserWordExclusionCommand(
  val userId: UUID,
  val term: String,
)
