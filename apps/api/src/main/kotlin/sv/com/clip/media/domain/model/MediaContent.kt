package sv.com.clip.media.domain.model

import org.jmolecules.ddd.annotation.AggregateRoot
import org.jmolecules.ddd.types.Identifier
import com.fasterxml.uuid.Generators
import java.time.LocalDateTime
import java.util.UUID

@JvmInline
value class MediaContentIdentifier(val value: UUID): Identifier {
  companion object {
    // El generador de UUIDv7 usa un timestamp en los primeros 48 bits
    private val generator = Generators.timeBasedEpochGenerator()

    fun generate(): MediaContentIdentifier {
      return MediaContentIdentifier(generator.generate())
    }

    fun fromString(uuid: String): MediaContentIdentifier {
      return MediaContentIdentifier(UUID.fromString(uuid))
    }
  }
}

@AggregateRoot
data class MediaContent(
  val id: MediaContentIdentifier = MediaContentIdentifier.generate(),
  val userId: UUID,
  val fileName: String,
  val originalFileName: String,
  val fileSize: Long,
  val mimeType: String,
  val mediaType: MediaType,
  val uploadedAt: LocalDateTime = LocalDateTime.now(),
  var metadata: MediaContentMetadata? = null,
) {}
