package sv.com.clip.auth.infrastructure.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID

@Component
class JwtService {
  private val secretKey = Keys.hmacShaKeyFor("clave-secreta-muy-larga-de-al-menos-32-bits".toByteArray())

  fun generateToken(userId: UUID, username: String, roles: List<String>): String =
    Jwts.builder()
      .subject(username)
      .claim("userId", userId.toString())
      .claim("roles", roles)
      .issuedAt(Date())
      .expiration(Date(System.currentTimeMillis() + 8_6400_000)) // 1 día
      .signWith(secretKey)
      .compact()

  fun validateToken(token: String): String? = getPayload(token).subject

/*  fun extractRoles(token: String): List<String> =
    getPayload(token)["roles"] as List<String>*/

  fun extractRoles(token: String): List<String> {
    val roles = getPayload(token)["roles"] as? List<*>
    return roles?.filterIsInstance<String>() ?: emptyList()
  }

  fun extractUserId(token: String): UUID =
    UUID.fromString(getPayload(token)["userId"] as String)

  private fun getPayload(token: String) = Jwts.parser()
    .verifyWith(secretKey)
    .build()
    .parseSignedClaims(token)
    .payload
}
