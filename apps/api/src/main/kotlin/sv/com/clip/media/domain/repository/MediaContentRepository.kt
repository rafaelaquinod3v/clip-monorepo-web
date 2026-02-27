package sv.com.clip.media.domain.repository

import sv.com.clip.media.domain.model.MediaContent
import sv.com.clip.media.domain.model.MediaType
import java.util.UUID

interface MediaContentRepository {
  fun save(media: MediaContent) : MediaContent
  fun findById(id : UUID, requestingUserId: UUID) : MediaContent?
  fun findAllByUserIdAndMediaType(userId: UUID, mediaType: MediaType) : List<MediaContent>
}
