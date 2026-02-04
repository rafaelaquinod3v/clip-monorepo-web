package sv.com.clip.user

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(private val userRepository: UserRepository) : UserDetailsService {
  override fun loadUserByUsername(username: String): UserDetails {
    val user = userRepository.findByUsername(username)
      ?: throw UsernameNotFoundException("Usuario no encontrado")

    return org.springframework.security.core.userdetails.User(
      user.username,
      user.password,
      user.roles.map { SimpleGrantedAuthority(it) }
    )
  }
}
