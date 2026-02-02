package sv.com.clip.user

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date

@Component
class JwtService {
  private val secretKey = Keys.hmacShaKeyFor("clave-secreta-muy-larga-de-al-menos-32-bits".toByteArray())

  fun generateToken(username: String, roles: List<String>): String =
    Jwts.builder()
      .subject(username)
      .claim("roles", roles)
      .issuedAt(Date())
      .expiration(Date(System.currentTimeMillis() + 8_6400_000)) // 1 día
      .signWith(secretKey)
      .compact()

  fun validateToken(token: String): String? =
    Jwts.parser().verifyWith(secretKey).build()
      .parseSignedClaims(token).payload.subject

  fun extractRoles(token: String): List<String> {
    val claims = Jwts.parser().verifyWith(secretKey).build()
      .parseSignedClaims(token).payload
    return claims["roles"] as List<String>
  }

}
