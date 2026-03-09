package sv.com.clip.user.application.command

data class RegisterUserCommand(
  val username: String,
  val password: String,
)
