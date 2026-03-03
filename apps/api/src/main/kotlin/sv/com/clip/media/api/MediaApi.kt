package sv.com.clip.media.api

import sv.com.clip.shared.pagination.PageQuery
import java.util.UUID

interface MediaApi {
  fun save(userId: UUID, bytes: ByteArray, fileName: String, originalFileName: String?): UUID
  fun findById(id : UUID, requestingUserId: UUID) : MediaResponse?
  fun findAllByUserIdAndMediaType(userId: UUID, mediaType: String) : List<MediaResponse>
  fun findAllByUserIdAndMediaTypePageable(userId: UUID, mediaType: String, pageQuery: PageQuery) : List<MediaResponse>
  fun findAllByUserIdAndMediaTypeInPageable(userId: UUID, mediaTypes: Collection<String>, pageQuery: PageQuery) : List<MediaResponse>
}
