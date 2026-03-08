package sv.com.clip.library.application.listeners

import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component
import sv.com.clip.library.application.services.ImportEpubService
import sv.com.clip.library.application.services.ImportPdfService
import sv.com.clip.media.api.MediaApi
import sv.com.clip.media.domain.events.MediaContentUploadedEvent

@Component
class MediaContentUploadedEventListener(
  private val mediaApi: MediaApi,
  private val importEpubService: ImportEpubService,
  private val importPdfService: ImportPdfService,
) {

  @ApplicationModuleListener
  fun on(event: MediaContentUploadedEvent) {
    println("MediaContentUploadedEvent: $event")
    val mediaResponse = mediaApi.findById(event.id, event.userId)
    when (mediaResponse?.mediaType) {
      "EPUB" -> {
        println("EPUB mediaResponse: $mediaResponse")
        importEpubService.processEpubToJsonl(mediaResponse.fileName)
      }
      "PDF" -> {
        println("PDF mediaResponse: $mediaResponse")
        importPdfService.generateJsonl(mediaResponse.fileName)
      }
      else -> println("Unknown mediaResponse: $mediaResponse")
    }
  }
}
