package sv.com.clip.config

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

class CustomUserDetails(
  val id: UUID,
  private val username: String,
  private val authorities: List<GrantedAuthority>
) : UserDetails {
  override fun getUsername() = username
  override fun getPassword() = null
  override fun getAuthorities() = authorities
  override fun isAccountNonExpired() = true
  override fun isAccountNonLocked() = true
}
