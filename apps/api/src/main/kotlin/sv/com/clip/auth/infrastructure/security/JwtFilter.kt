package sv.com.clip.auth.infrastructure.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.SignatureException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import sv.com.clip.auth.infrastructure.security.CustomUserDetails
import sv.com.clip.auth.infrastructure.security.JwtService

@Component
class JwtFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

  override fun doFilterInternal(
      request: HttpServletRequest,
      response: HttpServletResponse,
      filterChain: FilterChain
  ) {
    try {
      val authHeader = request.getHeader("Authorization")

      // 1. Verificar si la petición trae un Bearer Token
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response)
        return
      }

      val token = authHeader.substring(7)
      val username = jwtService.validateToken(token)

      // 2. Si el token es válido y el usuario no está ya autenticado en el contexto
      if (username != null && SecurityContextHolder.getContext().authentication == null) {

        val roles = jwtService.extractRoles(token) // Extraemos los roles del token
        val userId = jwtService.extractUserId(token)
        val authorities = roles.map { SimpleGrantedAuthority(it) } // Los convertimos para Spring
        val customUserDetails = CustomUserDetails(userId, username, authorities)
        val authToken = UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.authorities)
        authToken.details = WebAuthenticationDetailsSource().buildDetails(request)

        // 3. Registrar al usuario en el contexto de Spring para esta petición
        SecurityContextHolder.getContext().authentication = authToken
      }

      filterChain.doFilter(request, response)
    } catch (e: ExpiredJwtException) {
      handleAuthenticationError(response, "Token has expired", HttpServletResponse.SC_UNAUTHORIZED)
    } catch (e: SignatureException) {
      handleAuthenticationError(response, "Invalid token signature", HttpServletResponse.SC_FORBIDDEN)
    } catch (e: MalformedJwtException) {
      handleAuthenticationError(response, "Malformed token", HttpServletResponse.SC_FORBIDDEN)
    } catch (e: JwtException) {
      handleAuthenticationError(response, "Invalid token", HttpServletResponse.SC_FORBIDDEN)
    }
  }

  private fun handleAuthenticationError(response: HttpServletResponse, message: String, status: Int) {
    response.status = status
    response.contentType = "application/json"
    // Enviamos un JSON limpio para que tu App lo entienda
    response.writer.write("""{"error": "$message", "status": $status}""")
  }
}
