package sv.com.clip.media.application.services

import org.springframework.stereotype.Service
import sv.com.clip.media.api.MediaApi
import sv.com.clip.media.api.MediaRequest
import sv.com.clip.media.domain.model.MediaContent
import sv.com.clip.media.domain.model.MediaType
import sv.com.clip.media.infrastructure.adapter.MediaContentAdapter
import java.util.UUID

@Service
class MediaService(
  private val adapter: MediaContentAdapter
) : MediaApi {
  override fun save(media: MediaRequest): UUID {
    val m = MediaContent(
      fileName = media.fileName,
      originalFileName = media.originalFileName,
      fileSize = 100L,
      mimeType = "text/plain",
      mediaType = MediaType.EPUB,
    )
    return adapter.save(m).id.value
  }
}
