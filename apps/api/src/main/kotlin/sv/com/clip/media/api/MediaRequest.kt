package sv.com.clip.media.api

import sv.com.clip.media.domain.model.MediaType

data class MediaRequest(
  val fileName: String,
  val originalFileName: String,
  val fileSize: Long,
  val mimeType: String,
  val mediaType: MediaType,
)
