package sv.com.clip.user

import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID
import kotlin.String

@Entity
@Table(name = "users")
class User(
  @Column(unique = true)
  val username: String,
  var password: String,
  @ElementCollection(fetch = FetchType.EAGER)
  var roles: MutableSet<String> = mutableSetOf("ROLE_USER"),
  @Id
  var id: UUID? = UUID.randomUUID(),
) {
  // Constructor vacío requerido por Hibernate/JPA
  constructor() : this("", "", mutableSetOf("ROLE_USER"), UUID.randomUUID())
}
