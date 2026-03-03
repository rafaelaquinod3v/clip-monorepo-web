package sv.com.clip.media.domain.repository

import sv.com.clip.media.domain.model.MediaContent
import sv.com.clip.media.domain.model.MediaType
import sv.com.clip.shared.pagination.PageQuery
import java.util.UUID

interface MediaContentRepository {
  fun save(media: MediaContent) : MediaContent
  fun findById(id: UUID) : MediaContent?
  fun findByIdAndUserId(id : UUID, requestingUserId: UUID) : MediaContent?
  fun findAllByUserIdAndMediaType(userId: UUID, mediaType: MediaType) : List<MediaContent>
  fun findAllByUserIdAndMediaTypePageable(userId: UUID, mediaType: MediaType, pageQuery: PageQuery) : List<MediaContent>
  fun findAllByUserIdAndMediaTypeInPageable(userId: UUID, mediaTypes: Collection<MediaType>, pageQuery: PageQuery) : List<MediaContent>
}
