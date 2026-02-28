package sv.com.clip.library.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import sv.com.clip.media.api.MediaApi
import sv.com.clip.media.api.MediaResponse
import java.nio.file.Paths
import java.util.UUID

@Service
class EpubService(
  @Value("\${storage.location}") private val storageLocation: String,
  private val media: MediaApi,
  ) {

  private val root = Paths.get(storageLocation)
  private val mapper = jacksonObjectMapper()

  fun loadEpubFromJsonl(epubName: String, offset: Int, limit: Int): List<SentenceEntry> {
    val epubPath = root.resolve("$epubName.jsonl")
    val epubFile = epubPath.toFile()
    if (!epubFile.exists()) { throw RuntimeException("epub '$epubName' not found") }
    return epubFile.useLines { lines ->
      lines.drop(offset).take(limit).map { line ->
        mapper.readValue<SentenceEntry>(line)
      }.toList()
    }
  }

  fun loadEpubMediaContent(userId: UUID) : List<MediaResponse> {
    return media.findAllByUserIdAndMediaType(userId, "EPUB")
  }
}
