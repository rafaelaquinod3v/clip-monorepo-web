package sv.com.clip.media.api

data class MediaContentMetadataRequest(
  val contentType: String,
  val values: Map<String, Any?>
)
