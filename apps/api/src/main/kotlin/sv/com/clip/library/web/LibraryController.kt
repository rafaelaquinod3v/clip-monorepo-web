package sv.com.clip.library.web

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import sv.com.clip.config.CustomUserDetails
import sv.com.clip.library.application.services.EbookService
import sv.com.clip.library.application.SentenceEntry
import sv.com.clip.media.api.MediaResponse
import sv.com.clip.shared.pagination.PageQuery
import sv.com.clip.shared.pagination.SortOrder
import java.util.UUID

@RestController
@RequestMapping("/library")
class LibraryController(
  private val ebookService: EbookService,

) {

  @GetMapping("/{id}/content")
  fun getBookContent(
    @PathVariable id: String,
    @RequestParam(defaultValue = "0") offset: Int,
    @RequestParam(defaultValue = "20") limit: Int
  ): ResponseEntity<List<SentenceEntry>> {

    // Llamamos al servicio para obtener el fragmento de frases
    val nodes = ebookService.loadEpubFromJsonl(id, offset, limit)

    return if (nodes.isNotEmpty()) {
      ResponseEntity.ok(nodes)
    } else {
      // Si el offset es mayor al total de frases, devolvemos 204 (No Content)
      ResponseEntity.noContent().build()
    }
  }

  @GetMapping("/media-content/ebook")
  fun getEpubMediaContent(
    @AuthenticationPrincipal user: CustomUserDetails,
    @RequestParam(defaultValue = "0") offset: Int,
    @RequestParam(defaultValue = "10") limit: Int,
    @RequestParam(defaultValue = "createdAt") sortField: String,
    @RequestParam(defaultValue = "DESC") sortOrder: String,
    @RequestParam(required = true) mediaTypes: List<String> = listOf("EPUB"),
    ): ResponseEntity<List<MediaResponse>> {

    val pageQuery = PageQuery(offset, limit, sortField, SortOrder.valueOf(sortOrder))
    return ResponseEntity.ok(ebookService.loadEbookMediaContent(user.id, mediaTypes, pageQuery))
  }

  @GetMapping("/media-content/ebook/{id}")
  fun getEBook(
    @PathVariable id: UUID,
    @AuthenticationPrincipal user: CustomUserDetails,
  ) : ResponseEntity<MediaResponse> {
    return ResponseEntity.ok(ebookService.findEbookMediaContentById(id, user.id))
  }
}
