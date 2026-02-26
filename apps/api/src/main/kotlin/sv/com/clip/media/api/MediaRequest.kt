package sv.com.clip.media.api

import java.util.UUID

data class MediaRequest(
  val id: UUID,
  val fileName: String,
  val originalFileName: String,
)
