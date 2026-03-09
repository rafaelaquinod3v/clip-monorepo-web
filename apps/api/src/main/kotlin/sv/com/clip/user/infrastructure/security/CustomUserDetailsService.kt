package sv.com.clip.user.infrastructure.security

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import sv.com.clip.user.infrastructure.persistance.UserJpaRepository

@Service
class CustomUserDetailsService(private val userJpaRepository: UserJpaRepository) : UserDetailsService {
  override fun loadUserByUsername(username: String): UserDetails {
    val user = userJpaRepository.findByUsername(username)
      ?: throw UsernameNotFoundException("Usuario no encontrado")

    return User(
      user.username,
      user.password,
      user.roles.map { SimpleGrantedAuthority(it) }
    )
  }
}
