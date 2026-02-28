package sv.com.clip.media.domain.model

sealed class MediaContentMetadata

data class EpubMediaContentMetadata(
  val title: String,
  val author: String,
) : MediaContentMetadata()

data class PdfMediaContentMetadata(
  val title: String,
  val author: String,
) : MediaContentMetadata()

data class AudioMediaContentMetadata(
  val duration: Long,
  val bitrate: Int,
) : MediaContentMetadata()

data class VideoMediaContentMetadata(
  val duration: Long,
) : MediaContentMetadata()
