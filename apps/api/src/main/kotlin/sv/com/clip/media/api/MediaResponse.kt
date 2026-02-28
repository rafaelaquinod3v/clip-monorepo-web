package sv.com.clip.media.api

import java.time.LocalDateTime
import java.util.UUID

data class MediaResponse(
  val id: UUID,
  val fileName: String,
  val originalFilename: String,
  val fileSize: Long,
  val mimeType: String,
  val mediaType: String,
  val uploadedAt: LocalDateTime,
  val metadata: MediaContentMetadataDto?
)
