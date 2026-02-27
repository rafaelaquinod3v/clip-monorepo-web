package sv.com.clip.media.application.services

import org.apache.tika.Tika
import org.apache.tika.mime.MimeTypes
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import sv.com.clip.config.CustomUserDetails
import sv.com.clip.media.api.MediaApi
import sv.com.clip.media.api.MediaRequest
import sv.com.clip.media.api.MediaResponse
import sv.com.clip.media.domain.model.MediaContent
import sv.com.clip.media.domain.model.MediaType
import sv.com.clip.media.infrastructure.adapter.MediaContentAdapter
import java.util.UUID

@Service
class MediaService(
  private val adapter: MediaContentAdapter
) : MediaApi {

  private val tika = Tika()
  override fun save(bytes: ByteArray, fileName: String, originalFileName: String?): UUID {
    val mimeType = tika.detect(bytes)  // desde bytes, no inputStream
    val extension = MimeTypes.getDefaultMimeTypes().forName(mimeType).extension
    val mediaType = when {
      mimeType == "application/epub+zip" -> MediaType.EPUB
      mimeType == "application/pdf" -> MediaType.PDF
      mimeType.startsWith("audio/") -> MediaType.AUDIO
      mimeType.startsWith("video/") -> MediaType.VIDEO
      else -> throw IllegalArgumentException("Tipo de archivo no soportado: $mimeType")
    }

    val userId = (SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails)?.id
      ?: throw AuthenticationCredentialsNotFoundException("Usuario no autenticado")

    val originalFileName = originalFileName?.takeIf { it.isNotBlank() }
      ?: "$fileName$extension"

    val media = MediaContent(
      userId = userId,
      fileName = fileName,
      originalFileName = originalFileName,
      fileSize = bytes.size.toLong(),  // desde bytes
      mimeType = mimeType,
      mediaType = mediaType,
    )

    return adapter.save(media).id.value
  }

  override fun findById(id: UUID, requestingUserId: UUID): MediaResponse? {
    val media = adapter.findById(id, requestingUserId)
    if (media != null) {
      return MediaResponse(
        media.id.value,
        media.fileName,
        media.originalFileName,
        media.fileSize,
        media.mimeType,
        media.mediaType.name,
        media.uploadedAt
      )
    }
    return null
  }

  override fun findAllByUserIdAndMediaType(
    userId: UUID,
    mediaType: String
  ): List<MediaResponse> {
    return adapter.findAllByUserIdAndMediaType(userId, MediaType.valueOf(mediaType)).map { media ->
      MediaResponse(
        media.id.value,
        media.fileName,
        media.originalFileName,
        media.fileSize,
        media.mimeType,
        media.mediaType.name,
        media.uploadedAt
      )
    }
  }
}
