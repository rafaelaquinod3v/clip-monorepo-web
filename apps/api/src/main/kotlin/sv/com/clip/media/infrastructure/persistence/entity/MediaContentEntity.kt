package sv.com.clip.media.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import org.springframework.data.domain.Persistable
import sv.com.clip.media.domain.model.MediaContent
import sv.com.clip.media.domain.model.MediaContentIdentifier
import sv.com.clip.media.domain.model.MediaType
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "media_content")
class MediaContentEntity(
  @Id
  private val id: UUID,
  @Column(nullable = false)
  val userId: UUID,
  @Column(nullable = false)
  val fileName: String,
  @Column(nullable = false)
  val originalFileName: String,
  @Column(nullable = false)
  val fileSize: Long,
  @Column(nullable = false)
  val mimeType: String,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val mediaType: MediaType,
  @Column(nullable = false)
  val uploadedAt: LocalDateTime,
  ) : Persistable<UUID> {

  fun toDomain() : MediaContent {
    return MediaContent(
      MediaContentIdentifier(this.id),
      this.userId,
      this.fileName,
      this.originalFileName,
      this.fileSize,
      this.mimeType,
      this.mediaType,
      this.uploadedAt,
    )
  }

  companion object {
    fun fromDomain(mediaContent: MediaContent): MediaContentEntity {
      return MediaContentEntity(
        mediaContent.id.value,
        mediaContent.userId,
        mediaContent.fileName,
        mediaContent.originalFileName,
        mediaContent.fileSize,
        mediaContent.mimeType,
        mediaContent.mediaType,
        mediaContent.uploadedAt,
      )
    }
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
