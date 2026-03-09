package sv.com.clip.user.domain

interface UserRepository {
  fun findByUsername(username: String): User?
  fun findAll(): List<User>
  fun save(user: User): User
}
