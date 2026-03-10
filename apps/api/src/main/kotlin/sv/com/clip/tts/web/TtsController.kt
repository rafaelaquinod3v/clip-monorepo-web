package sv.com.clip.tts.web

import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.async.AsyncWebRequest
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import sv.com.clip.tts.domain.TtsPort

@RestController
@RequestMapping("/api/v1/tts")
class TtsController(private val ttsPort: TtsPort) {

  @PostMapping("/stream")
  fun streamTextToNDJson(
    asyncRequest: AsyncWebRequest,
    @RequestBody request: Map<String, String>
  ): ResponseEntity<StreamingResponseBody> {
    asyncRequest.setTimeout(300000L) // 5 minutos solo para este endpoint
    val fullText = request["text"] ?: throw IllegalArgumentException("Text is required")
    val voice = request["voice"] ?: "af_heart"

    val responseBody = StreamingResponseBody { outputStream ->
      // Delegamos TODA la lógica al servicio
      ttsPort.textSplitBySentenceSync(fullText, voice, outputStream)
    }

    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("application/x-ndjson"))
      .body(responseBody)
  }
}
