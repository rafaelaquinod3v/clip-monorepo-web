package sv.com.clip.user.infrastructure

import org.springframework.stereotype.Service
import sv.com.clip.user.api.UserAuthView
import sv.com.clip.user.api.UserProvider
import sv.com.clip.user.infrastructure.persistance.UserJpaRepository
import sv.com.clip.user.infrastructure.persistance.UserRepositoryAdapter

@Service
class UserProviderImpl(
  private val userRepositoryAdapter: UserRepositoryAdapter,
) : UserProvider {
  override fun getAuthViewByUsername(username: String): UserAuthView? {
    return userRepositoryAdapter.findByUsername(username)?.let { UserAuthView(it.id.value, it.username, it.password, it.roles) }
  }
}
