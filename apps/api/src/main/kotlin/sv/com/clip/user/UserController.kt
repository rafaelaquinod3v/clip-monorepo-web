package sv.com.clip.user

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users")
class UserController(
  private val userRepository: UserRepository,
  private val passwordEncoder: PasswordEncoder
) {
  @PostMapping("/register")
  fun register(@RequestBody user: User): User {
    // Encriptar la contraseña antes de guardar
    user.password = passwordEncoder.encode(user.password)!!
    user.id = UUID.randomUUID()
    return userRepository.save(user)
  }

  @GetMapping("/list")
  fun listAll(): List<User> = userRepository.findAll()
}
