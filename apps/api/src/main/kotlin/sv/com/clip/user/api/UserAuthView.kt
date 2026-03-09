package sv.com.clip.user.api

import java.util.UUID

data class UserAuthView(
  val id: UUID,
  val username: String,
  val password: String,
  val roles: List<String>,
)
