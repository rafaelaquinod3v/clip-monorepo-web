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
  @Id
  var id: UUID,
  @Column(unique = true)
  val username: String,
  var password: String,
  @ElementCollection(fetch = FetchType.EAGER)
  val roles: Set<String> = setOf("ROLE_USER")
) {}
