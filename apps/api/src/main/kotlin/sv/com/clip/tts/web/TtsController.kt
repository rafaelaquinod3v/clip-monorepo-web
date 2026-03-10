package sv.com.clip.tts.web

import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.async.AsyncWebRequest
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import sv.com.clip.tts.infrastructure.TtsAdapter

@RestController
@RequestMapping("/api/v1/tts")
class TtsController(private val ttsAdapter: TtsAdapter,) {

/*  @GetMapping("/speak", produces = ["audio/mpeg"])
  fun generateMp3Audio(@RequestParam text: String): ResponseEntity<ByteArray> {
    val mp3Data = ttsService.generateMp3(text)

    val headers = HttpHeaders()
    headers.contentType = MediaType.parseMediaType("audio/mpeg")
    headers.contentDisposition = ContentDisposition.inline().filename("speech.mp3").build()

    return ResponseEntity(mp3Data, headers, HttpStatus.OK)
  }*/

/*  @GetMapping("/synthesize")
  fun synthesize(@RequestParam text: String): ResponseEntity<Map<String, Any>> {
    val result = ttsService.generateAudioWithSync(text)
    return ResponseEntity.ok(result)
  }*/

  @PostMapping("/stream")
  fun streamTextToNDJson(
    asyncRequest: AsyncWebRequest,
    @RequestBody request: Map<String, String>
  ): ResponseEntity<StreamingResponseBody> {
    asyncRequest.setTimeout(300000L) // 5 minutos solo para este endpoint
    val fullText = request["text"] ?: throw IllegalArgumentException("Text is required")
    val voice = request["voice"] ?: "af_heart"

    println(fullText)

    val responseBody = StreamingResponseBody { outputStream ->
      // Delegamos TODA la lógica al servicio
      ttsAdapter.streamTextSplitBySentenceSync(fullText, voice, outputStream)
    }

    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("application/x-ndjson"))
      .body(responseBody)
  }
}
