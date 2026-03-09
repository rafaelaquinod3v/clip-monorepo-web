package sv.com.clip.user.infrastructure.persistance

import org.springframework.stereotype.Service
import sv.com.clip.user.domain.User
import sv.com.clip.user.domain.UserRepository

@Service
class UserRepositoryAdapter(
  private val userJpaRepository: UserJpaRepository,
) : UserRepository {

  override fun findByUsername(username: String): User? {
    return userJpaRepository.findByUsername(username)?.toDomain()
  }

  override fun findAll(): List<User> {
    return userJpaRepository.findAll().map { it.toDomain() }
  }

  override fun save(user: User): User {
    return userJpaRepository.save(UserJpaEntity.fromDomain(user)).toDomain()
  }
}
