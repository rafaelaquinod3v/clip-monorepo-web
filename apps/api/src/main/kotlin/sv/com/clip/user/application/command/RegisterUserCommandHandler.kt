package sv.com.clip.user.application.command

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import sv.com.clip.user.domain.User
import sv.com.clip.user.infrastructure.persistance.UserRepositoryAdapter

@Service
class RegisterUserCommandHandler(
  private val userRepositoryAdapter: UserRepositoryAdapter,
  private val passwordEncoder: PasswordEncoder,
) {
  fun handle(command: RegisterUserCommand): User {
    val encodedPassword = passwordEncoder.encode(command.password)
      ?: throw IllegalArgumentException("El password no puede ser nulo")
    return userRepositoryAdapter.save(User(username = command.username, password = encodedPassword));
  }
}
