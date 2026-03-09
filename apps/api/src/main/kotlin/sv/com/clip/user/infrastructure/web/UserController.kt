package sv.com.clip.user.infrastructure.web

import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sv.com.clip.user.application.command.RegisterUserCommand
import sv.com.clip.user.application.command.RegisterUserCommandHandler
import sv.com.clip.user.domain.User
import sv.com.clip.user.infrastructure.persistance.UserJpaEntity
import sv.com.clip.user.infrastructure.persistance.UserJpaRepository
import sv.com.clip.user.infrastructure.persistance.UserRepositoryAdapter
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class UserController(
  private val userAdapter: UserRepositoryAdapter,
  private val registerUserCommandHandler: RegisterUserCommandHandler
) {

  @PostMapping("/register")
  fun register(@RequestBody credentials: RegisterUserRequest): ResponseEntity<Any> {
    val command = RegisterUserCommand(credentials.username, credentials.password)
    return ResponseEntity.ok(registerUserCommandHandler.handle(command))
  }

  @GetMapping("/list")
  fun listAll(): List<User> = userAdapter.findAll()



}
