package sv.com.clip.user.domain

import com.fasterxml.uuid.Generators
import org.jmolecules.ddd.annotation.AggregateRoot
import org.jmolecules.ddd.types.Identifier
import java.util.UUID

@JvmInline
value class UserIdentifier(val value: UUID): Identifier {
  companion object {
    // El generador de UUIDv7 usa un timestamp en los primeros 48 bits
    private val generator = Generators.timeBasedEpochGenerator()

    fun generate(): UserIdentifier {
      return UserIdentifier(generator.generate())
    }

    fun fromString(uuid: String): UserIdentifier {
      return UserIdentifier(UUID.fromString(uuid))
    }
  }
}

@AggregateRoot
class User(
  val id: UserIdentifier = UserIdentifier.generate(),
  val username: String,
  val password: String,
  val roles: List<String> = listOf("ROLE_USER"),
) {}
