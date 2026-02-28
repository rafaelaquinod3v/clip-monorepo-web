package sv.com.clip.media.application.services

import org.apache.tika.Tika
import org.apache.tika.mime.MimeTypes
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import sv.com.clip.config.CustomUserDetails
import sv.com.clip.media.api.MediaApi
import sv.com.clip.media.api.MediaContentMetadataDto
import sv.com.clip.media.api.MediaContentMetadataRequest
import sv.com.clip.media.api.MediaResponse
import sv.com.clip.media.domain.model.AudioMediaContentMetadata
import sv.com.clip.media.domain.model.EpubMediaContentMetadata
import sv.com.clip.media.domain.model.MediaContent
import sv.com.clip.media.domain.model.MediaContentMetadata
import sv.com.clip.media.domain.model.MediaType
import sv.com.clip.media.domain.model.PdfMediaContentMetadata
import sv.com.clip.media.domain.model.VideoMediaContentMetadata
import sv.com.clip.media.infrastructure.adapter.MediaContentAdapter
import java.util.UUID

@Service
class MediaService(
  private val adapter: MediaContentAdapter
) : MediaApi {

  private val tika = Tika()
  override fun save(bytes: ByteArray, fileName: String, originalFileName: String?, metadata: MediaContentMetadataRequest?): UUID {

    val mimeType = tika.detect(bytes)  // desde bytes, no inputStream
    val extension = MimeTypes.getDefaultMimeTypes().forName(mimeType).extension
    val mediaType = buildMediaType(mimeType)
    val domainMetadata = metadata?.let { buildMediaContentMetadata(it) }

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
      metadata = domainMetadata
    )

    return adapter.save(media).id.value
  }

  override fun findById(id: UUID, requestingUserId: UUID): MediaResponse? {
    val media = adapter.findByIdAndUserId(id, requestingUserId)
    if (media != null) {
      return MediaResponse(
        media.id.value,
        media.fileName,
        media.originalFileName,
        media.fileSize,
        media.mimeType,
        media.mediaType.name,
        media.uploadedAt,
        buildMediaContentMetadataDto(media)
      )
    }
    return null
  }

  override fun findAllByUserIdAndMediaType(
    userId: UUID,
    mediaType: String
  ): List<MediaResponse> {
    return adapter.findAllByUserIdAndMediaType(userId, MediaType.valueOf(mediaType)).map { media ->

      val metadata = buildMediaContentMetadataDto(media)

      MediaResponse(
        media.id.value,
        media.fileName,
        media.originalFileName,
        media.fileSize,
        media.mimeType,
        media.mediaType.name,
        media.uploadedAt,
        metadata
      )
    }
  }

  private fun buildMediaContentMetadata(metadata: MediaContentMetadataRequest): MediaContentMetadata {
    fun Map<String, Any?>.getString(key: String): String =
      this[key]?.toString()?.takeIf { it.isNotBlank() } ?: ""

    return when (metadata.contentType) {
      "EPUB" -> EpubMediaContentMetadata(
        title = metadata.values.getString("title"),
        author = metadata.values.getString("author"),
      )
      "PDF" -> PdfMediaContentMetadata(
        title = metadata.values.getString("title"),
        author = metadata.values.getString("author"),
      )
      else -> throw IllegalArgumentException("Tipo no soportado: ${metadata.contentType}")
    }
  }

  private fun buildMediaType(mimeType: String): MediaType {
    return when {
      mimeType == "application/epub+zip" -> MediaType.EPUB
      mimeType == "application/pdf" -> MediaType.PDF
      mimeType.startsWith("audio/") -> MediaType.AUDIO
      mimeType.startsWith("video/") -> MediaType.VIDEO
      else -> throw IllegalArgumentException("Tipo de archivo no soportado: $mimeType")
    }
  }

  private fun buildMediaContentMetadataDto(media: MediaContent) : MediaContentMetadataDto?{
    return media.metadata?.let {
      when (it) {
        is EpubMediaContentMetadata  -> MediaContentMetadataDto("EPUB",  mapOf("title" to it.title, "author" to it.author))
        is PdfMediaContentMetadata   -> MediaContentMetadataDto("PDF",   mapOf("title" to it.title, "author" to it.author))
        is AudioMediaContentMetadata -> MediaContentMetadataDto("AUDIO", mapOf("duration" to it.duration, "bitrate" to it.bitrate))
        is VideoMediaContentMetadata -> MediaContentMetadataDto("VIDEO", mapOf("duration" to it.duration))
      }
    }
  }
}
