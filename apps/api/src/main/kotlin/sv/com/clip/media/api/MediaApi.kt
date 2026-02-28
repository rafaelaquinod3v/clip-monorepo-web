package sv.com.clip.media.api

import java.util.UUID

interface MediaApi {
  fun save(bytes: ByteArray, fileName: String, originalFileName: String?, metadata: MediaContentMetadataRequest?): UUID
  fun findById(id : UUID, requestingUserId: UUID) : MediaResponse?
  fun findAllByUserIdAndMediaType(userId: UUID, mediaType: String) : List<MediaResponse>
}
