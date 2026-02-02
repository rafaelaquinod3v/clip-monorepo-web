package sv.com.clip.user

import org.springframework.http.ResponseEntity
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
  private val passwordEncoder: PasswordEncoder,
  private val jwtService: JwtService
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

  @PostMapping("/login")
  fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
    // 1. Buscar usuario por username
    val user = userRepository.findByUsername(request.username)
      ?: return ResponseEntity.status(401).body("Usuario no encontrado")

    // 2. Verificar password con BCrypt
    if (!passwordEncoder.matches(request.password, user.password)) {
      return ResponseEntity.status(401).body("Contraseña incorrecta")
    }

    // 3. Generar Token
    val token = jwtService.generateToken(user.username, user.roles.toList())

    return ResponseEntity.ok(LoginResponse(token))
  }

}
