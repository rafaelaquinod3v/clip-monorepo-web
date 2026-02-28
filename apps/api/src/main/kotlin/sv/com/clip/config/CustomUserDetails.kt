package sv.com.clip.config

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

class CustomUserDetails(
  val id: UUID,
  username: String,
  authorities: List<GrantedAuthority>
) : User(username, "", authorities) // password can be empty for JWT


/*class CustomUserDetails(
  val id: UUID,
  private val username: String,
  private val authorities: List<GrantedAuthority>
) : UserDetails {
  override fun getUsername() = username
  override fun getPassword() = null
  override fun getAuthorities() = authorities
  override fun isAccountNonExpired() = true
  override fun isAccountNonLocked() = true
  override fun isCredentialsNonExpired() = true
  override fun isEnabled() = true
}*/
