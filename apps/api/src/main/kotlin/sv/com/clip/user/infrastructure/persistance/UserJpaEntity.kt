package sv.com.clip.user.infrastructure.persistance

import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import org.springframework.data.domain.Persistable
import sv.com.clip.user.domain.User
import sv.com.clip.user.domain.UserIdentifier
import java.util.UUID

@Entity
@Table(name = "users")
class UserJpaEntity(
  @Id
  private val id: UUID,
  @Column(unique = true)
  val username: String,
  val password: String,
  @ElementCollection(fetch = FetchType.EAGER)
  val roles: List<String>,
) : Persistable<UUID> {

  companion object {
    fun fromDomain(user: User): UserJpaEntity {
      return UserJpaEntity(
        user.id.value,
        user.username,
        user.password,
        user.roles
      )
    }
  }

  fun toDomain(): User {
    return User(
      UserIdentifier(id),
      username,
      password,
      roles,
    )
  }
  @Transient
  private var isNewEntity: Boolean = true

  // --- Implementación de Persistable ---

  override fun getId(): UUID = id

  override fun isNew(): Boolean = isNewEntity

  // Metodo para marcar que la entidad ya existe (usado al cargar de DB)
  @PostLoad
  @PostPersist
  fun markNotNew() {
    this.isNewEntity = false
  }
}
