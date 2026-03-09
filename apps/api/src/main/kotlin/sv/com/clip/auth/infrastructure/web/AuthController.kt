package sv.com.clip.auth.infrastructure.web

import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sv.com.clip.auth.infrastructure.security.JwtService
import sv.com.clip.user.api.UserProvider

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
  private val userProvider: UserProvider,
  private val passwordEncoder: PasswordEncoder,
  private val jwtService: JwtService
) {
  @PostMapping("/login")
  fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
    // 1. Buscar usuario por username
    val userAuthView = userProvider.getAuthViewByUsername(request.username)
      ?: return ResponseEntity.status(401).body("Usuario no encontrado")

    // 2. Verificar password con BCrypt
    if (!passwordEncoder.matches(request.password, userAuthView.password)) {
      return ResponseEntity.status(401).body("Contraseña incorrecta")
    }

    // 3. Generar Token
    val token = jwtService.generateToken(userAuthView.id, userAuthView.username, userAuthView.roles)

    return ResponseEntity.ok(AuthResponse(token))
  }
}
