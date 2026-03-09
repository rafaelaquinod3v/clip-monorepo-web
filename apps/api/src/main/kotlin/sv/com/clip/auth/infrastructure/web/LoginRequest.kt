package sv.com.clip.auth.infrastructure.web

data class LoginRequest(
  val username: String,
  val password: String
)
