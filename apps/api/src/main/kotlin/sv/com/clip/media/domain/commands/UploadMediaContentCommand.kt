package sv.com.clip.media.domain.commands

import java.util.UUID

data class UploadMediaContentCommand(
    val userId: UUID,
    val file: ByteArray,
    val originalFileName: String?,
  ) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as UploadMediaContentCommand

    if (userId != other.userId) return false
    if (!file.contentEquals(other.file)) return false
    if (originalFileName != other.originalFileName) return false

    return true
  }

  override fun hashCode(): Int {
    var result = userId.hashCode()
    result = 31 * result + file.contentHashCode()
    result = 31 * result + (originalFileName?.hashCode() ?: 0)
    return result
  }
}
