package sv.com.clip.user

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
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
      val authorities = roles.map { SimpleGrantedAuthority(it) } // Los convertimos para Spring

      val authToken = UsernamePasswordAuthenticationToken(
        username,
        null,
        authorities // <--- Ahora pasamos la lista de roles real
      )
      // Aquí podrías cargar roles reales desde la DB si los necesitas
/*      val authToken = UsernamePasswordAuthenticationToken(
        username, null, emptyList()
      )*/

      authToken.details = WebAuthenticationDetailsSource().buildDetails(request)

      // 3. Registrar al usuario en el contexto de Spring para esta petición
      SecurityContextHolder.getContext().authentication = authToken
    }

    filterChain.doFilter(request, response)
  }
}
