package sv.com.clip.library.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import sv.com.clip.library.api.ImportRawTextSourceMaterialRequest
import sv.com.clip.library.application.ImportEpubService

@RestController
@RequestMapping("/library")
class LibraryController(private val importEpubService: ImportEpubService) {

  @PostMapping("/import")
  fun addToLibrary(@RequestBody request: ImportRawTextSourceMaterialRequest): Boolean {
    println(request.rawText)
    return true
  }

  @PostMapping("/upload/epub")
  fun uploadEpub(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
    // 1. Validar que no esté vacío
    if (file.isEmpty) {
      return ResponseEntity("El archivo está vacío", HttpStatus.BAD_REQUEST)
    }

    // 2. Validar que sea un EPUB (opcional pero recomendado)
    val contentType = file.contentType
    if (contentType != "application/epub+zip") {
      return ResponseEntity("Solo se permiten archivos EPUB", HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    }

    // 3. Procesar el archivo (guardar en disco, DB o S3)
    // val fileName = file.originalFilename
    val fileName =  importEpubService.save(file)
    println("Recibido ebook: $fileName de tamaño ${file.size} bytes")
    val jsonlFileName = importEpubService.processEpubToJsonl(fileName)
    println("Recibido jsonl: $jsonlFileName")
    // 2. Definir ruta del JSONL de salida

    return ResponseEntity("Ebook '$fileName' subido con éxito", HttpStatus.OK)
  }

  //@PostMapping("process")
  //suspend fun importNews(@RequestBody request: ImportRequest): ResponseEntity<ContentResponse> {
    //val result = learningService.processContent(request.url)
    //return ResponseEntity.ok(ContentResponse("result"))
  //}
}
