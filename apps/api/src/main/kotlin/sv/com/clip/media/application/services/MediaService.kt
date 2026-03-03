package sv.com.clip.media.application.services

import nl.siegmann.epublib.domain.Author
import nl.siegmann.epublib.epub.EpubReader
import org.apache.pdfbox.Loader
import org.apache.tika.Tika
import org.apache.tika.mime.MimeTypes
import org.springframework.stereotype.Service
import sv.com.clip.media.api.MediaApi
import sv.com.clip.media.api.MediaContentMetadataDto
import sv.com.clip.media.api.MediaResponse
import sv.com.clip.media.domain.model.AudioMediaContentMetadata
import sv.com.clip.media.domain.model.EpubMediaContentMetadata
import sv.com.clip.media.domain.model.MediaContent
import sv.com.clip.media.domain.model.MediaContentMetadata
import sv.com.clip.media.domain.model.MediaType
import sv.com.clip.media.domain.model.PdfMediaContentMetadata
import sv.com.clip.media.domain.model.VideoMediaContentMetadata
import sv.com.clip.media.infrastructure.adapter.MediaContentAdapter
import sv.com.clip.shared.pagination.PageQuery
import ws.schild.jave.MultimediaObject
import java.io.File
import java.util.UUID

@Service
class MediaService(
  private val adapter: MediaContentAdapter
) : MediaApi {

  private val tika = Tika()

  private fun getMediaType(bytes: ByteArray): MediaType = buildMediaType(tika.detect(bytes))

  override fun save(
    userId: UUID,
    bytes: ByteArray,
    fileName: String,
    originalFileName: String?
  ): UUID {

    val mimeType = tika.detect(bytes)
    val extension = MimeTypes.getDefaultMimeTypes().forName(mimeType).extension
    val mediaType = buildMediaType(mimeType)

    val domainMetadata = extractMetadata(bytes, mediaType)

    val originalFileName = originalFileName?.takeIf { it.isNotBlank() }
      ?: "$fileName$extension"

    val media = MediaContent(
      userId = userId,
      fileName = fileName,
      originalFileName = originalFileName,
      fileSize = bytes.size.toLong(),
      mimeType = mimeType,
      mediaType = mediaType,
      metadata = domainMetadata
    )

    return adapter.save(media).id.value
  }

  override fun findById(id: UUID, requestingUserId: UUID): MediaResponse? {
    val media = adapter.findByIdAndUserId(id, requestingUserId)
    if (media != null) {
      return MediaResponse(
        media.id.value,
        media.fileName,
        media.originalFileName,
        media.fileSize,
        media.mimeType,
        media.mediaType.name,
        media.uploadedAt,
        buildMediaContentMetadataDto(media)
      )
    }
    return null
  }

  override fun findAllByUserIdAndMediaType(
    userId: UUID,
    mediaType: String,
  ): List<MediaResponse> {
    return adapter.findAllByUserIdAndMediaType(userId, MediaType.valueOf(mediaType)).map { media ->

      val metadata = buildMediaContentMetadataDto(media)

      MediaResponse(
        media.id.value,
        media.fileName,
        media.originalFileName,
        media.fileSize,
        media.mimeType,
        media.mediaType.name,
        media.uploadedAt,
        metadata
      )
    }
  }

  override fun findAllByUserIdAndMediaTypePageable(
    userId: UUID,
    mediaType: String,
    pageQuery: PageQuery
  ): List<MediaResponse> {
    return adapter.findAllByUserIdAndMediaTypePageable(userId, MediaType.valueOf(mediaType), pageQuery).map { media ->

      val metadata = buildMediaContentMetadataDto(media)

      MediaResponse(
        media.id.value,
        media.fileName,
        media.originalFileName,
        media.fileSize,
        media.mimeType,
        media.mediaType.name,
        media.uploadedAt,
        metadata
      )
    }
  }

  override fun findAllByUserIdAndMediaTypeInPageable(
    userId: UUID,
    mediaTypes: Collection<String>,
    pageQuery: PageQuery
  ): List<MediaResponse> {
    return adapter.findAllByUserIdAndMediaTypeInPageable(userId, mediaTypes.map { MediaType.valueOf(it) }, pageQuery).map { media ->

      val metadata = buildMediaContentMetadataDto(media)

      MediaResponse(
        media.id.value,
        media.fileName,
        media.originalFileName,
        media.fileSize,
        media.mimeType,
        media.mediaType.name,
        media.uploadedAt,
        metadata
      )
    }
  }

  private fun extractMetadata(bytes: ByteArray, mediaType: MediaType): MediaContentMetadata? {
    return when (mediaType) {
      MediaType.EPUB -> extractEpubMetadata(bytes)
      MediaType.PDF -> extractPdfMetadata(bytes)
      MediaType.AUDIO -> extractAudioMetadata(bytes)
      MediaType.VIDEO -> extractVideoMetadata(bytes)
    }
  }

  private fun extractEpubMetadata(bytes: ByteArray): EpubMediaContentMetadata {
    val book = EpubReader().readEpub(bytes.inputStream())
    val title = book.title.orEmpty().trim()
    val author = book.metadata.authors.firstOrNull()?.fullName().orEmpty()
    return EpubMediaContentMetadata(title, author)
  }

  private fun extractPdfMetadata(bytes: ByteArray): PdfMediaContentMetadata {
    Loader.loadPDF(bytes).use { document ->
      val info = document.documentInformation
      val title = info.title.orEmpty().trim()
      val author = info.author.orEmpty().trim()
      return PdfMediaContentMetadata(title, author)
    }
  }

  private fun extractAudioMetadata(bytes: ByteArray): AudioMediaContentMetadata {
    val tempFile = File.createTempFile("audio_", ".tmp").also { it.deleteOnExit() }
    return try {
      tempFile.writeBytes(bytes)
      val info = MultimediaObject(tempFile).info
      AudioMediaContentMetadata(
        duration = info.duration,
        bitrate  = info.audio.bitRate,
      )
    } finally {
      tempFile.delete()
    }
  }


  private fun extractVideoMetadata(bytes: ByteArray): VideoMediaContentMetadata {
    val tempFile = File.createTempFile("video_", ".tmp").also { it.deleteOnExit() }
    return try {
      tempFile.writeBytes(bytes)
      val info = MultimediaObject(tempFile).info
      VideoMediaContentMetadata(
        duration = info.duration,
      )
    } finally {
      tempFile.delete()
    }
  }

  private fun Author.fullName(): String =
    listOfNotNull(
      firstname?.takeIf { it.isNotBlank() },
      lastname?.takeIf { it.isNotBlank() }
    ).joinToString(" ")

  private fun buildMediaType(mimeType: String): MediaType {
    return when {
      mimeType == "application/epub+zip" -> MediaType.EPUB
      mimeType == "application/pdf" -> MediaType.PDF
      mimeType.startsWith("audio/") -> MediaType.AUDIO
      mimeType.startsWith("video/") -> MediaType.VIDEO
      else -> throw IllegalArgumentException("Tipo de archivo no soportado: $mimeType")
    }
  }

  private fun buildMediaContentMetadataDto(media: MediaContent) : MediaContentMetadataDto?{
    return media.metadata?.let {
      when (it) {
        is EpubMediaContentMetadata  -> MediaContentMetadataDto("EPUB",  mapOf("title" to it.title, "author" to it.author))
        is PdfMediaContentMetadata   -> MediaContentMetadataDto("PDF",   mapOf("title" to it.title, "author" to it.author))
        is AudioMediaContentMetadata -> MediaContentMetadataDto("AUDIO", mapOf("duration" to it.duration, "bitrate" to it.bitrate))
        is VideoMediaContentMetadata -> MediaContentMetadataDto("VIDEO", mapOf("duration" to it.duration))
      }
    }
  }
}
