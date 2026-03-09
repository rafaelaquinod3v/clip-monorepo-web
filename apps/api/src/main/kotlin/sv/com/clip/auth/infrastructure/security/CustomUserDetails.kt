package sv.com.clip.auth.infrastructure.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import java.util.UUID

class CustomUserDetails(
    val id: UUID,
    username: String,
    authorities: List<GrantedAuthority>
) : User(username, "", authorities) // password can be empty for JWT
