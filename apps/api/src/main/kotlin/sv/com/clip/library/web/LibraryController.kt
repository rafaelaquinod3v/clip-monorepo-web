package sv.com.clip.library.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import sv.com.clip.config.CustomUserDetails
import sv.com.clip.library.api.ImportRawTextSourceMaterialRequest
import sv.com.clip.library.application.EpubService
import sv.com.clip.library.application.ImportEpubService
import sv.com.clip.library.application.SentenceEntry
import sv.com.clip.media.api.MediaResponse
import java.security.Principal

@RestController
@RequestMapping("/library")
class LibraryController(
  private val importEpubService: ImportEpubService,
  private val epubService: EpubService,
) {

  @PostMapping("/import")
  fun addToLibrary(@RequestBody request: ImportRawTextSourceMaterialRequest): Boolean {
    println(request.rawText)
    return true
  }

  @PostMapping("/upload/epub")
  fun uploadEpub(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {

    if (file.isEmpty) {
      return ResponseEntity("El archivo está vacío", HttpStatus.BAD_REQUEST)
    }

    // 3. Procesar el archivo (guardar en disco, DB o S3)

    val fileName =  importEpubService.save(file)
    println("Recibido ebook: $fileName de tamaño ${file.size} bytes")
    // val jsonlFileName = importEpubService.processEpubToJsonl(fileName)
    //println("Recibido jsonl: $jsonlFileName")
    // 2. Definir ruta del JSONL de salida

    return ResponseEntity("Ebook '$fileName' subido con éxito", HttpStatus.OK)
  }

  //@PostMapping("process")
  //suspend fun importNews(@RequestBody request: ImportRequest): ResponseEntity<ContentResponse> {
    //val result = learningService.processContent(request.url)
    //return ResponseEntity.ok(ContentResponse("result"))
  //}

  @GetMapping("/{id}/content")
  fun getBookContent(
    @PathVariable id: String,
    @RequestParam(defaultValue = "0") offset: Int,
    @RequestParam(defaultValue = "20") limit: Int
  ): ResponseEntity<List<SentenceEntry>> {

    // Llamamos al servicio para obtener el fragmento de frases
    val nodes = epubService.loadEpubFromJsonl(id, offset, limit)

    return if (nodes.isNotEmpty()) {
      ResponseEntity.ok(nodes)
    } else {
      // Si el offset es mayor al total de frases, devolvemos 204 (No Content)
      ResponseEntity.noContent().build()
    }
  }

  @GetMapping("/epub/media-content")
  fun getEpubMediaContent(
    @AuthenticationPrincipal user: CustomUserDetails,
    @RequestParam(defaultValue = "0") offset: Int,
    @RequestParam(defaultValue = "10") limit: Int,
    @RequestParam(defaultValue = "desc") sortOrder: String,
    ): ResponseEntity<List<MediaResponse>> {
    println("Get epub media content $offset, $limit, $sortOrder")
    return ResponseEntity.ok(epubService.loadEpubMediaContent(user.id))
  }
}
