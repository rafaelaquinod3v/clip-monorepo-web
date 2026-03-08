package sv.com.clip.library.application.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import sv.com.clip.library.application.SentenceEntry
import sv.com.clip.media.api.MediaApi
import sv.com.clip.media.api.MediaResponse
import sv.com.clip.media.domain.model.MediaType
import sv.com.clip.shared.pagination.PageQuery
import java.nio.file.Paths
import java.util.UUID

@Service
class EbookService(
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

  fun loadEbookMediaContent(userId: UUID, mediaTypes: Collection<String>, pageQuery: PageQuery) : List<MediaResponse> {
    return media.findAllByUserIdAndMediaTypeInPageable(userId, mediaTypes, pageQuery)
  }

  fun findEbookMediaContentById(id: UUID, userId: UUID): MediaResponse? {
    return media.findById(id, userId)
  }
}
