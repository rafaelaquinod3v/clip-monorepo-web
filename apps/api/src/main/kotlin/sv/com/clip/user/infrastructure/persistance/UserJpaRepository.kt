package sv.com.clip.user.infrastructure.persistance

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserJpaRepository : JpaRepository<UserJpaEntity, UUID> {
  fun findByUsername(username: String): UserJpaEntity?
}
