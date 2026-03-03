package sv.com.clip.media.domain.events

import java.util.UUID

data class MediaContentUploadedEvent(
  val userId: UUID,
  val id: UUID,
)
