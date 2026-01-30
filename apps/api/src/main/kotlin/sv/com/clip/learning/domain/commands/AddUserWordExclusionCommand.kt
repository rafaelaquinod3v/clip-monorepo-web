package sv.com.clip.learning.domain.commands

import java.util.UUID

data class AddUserWordExclusionCommand(
  val userId: UUID,
  val lemma: String,
)
