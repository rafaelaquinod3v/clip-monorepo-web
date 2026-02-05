package sv.com.clip.speech.web

import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import sv.com.clip.speech.internal.TtsService

@RestController
@RequestMapping("/api/audio")
class AudioController(private val ttsService: TtsService) {

  @GetMapping("/speak", produces = ["audio/wav"])
  fun speak(@RequestParam text: String): ResponseEntity<ByteArray> {
    val wavData = ttsService.generateWav(text)

    val headers = HttpHeaders()
    headers.contentType = MediaType.parseMediaType("audio/wav")
    headers.contentDisposition = ContentDisposition.inline().filename("speech.wav").build()

    return ResponseEntity(wavData, headers, HttpStatus.OK)
  }

  @GetMapping("/synthesize")
  fun synthesize(@RequestParam text: String): ResponseEntity<Map<String, Any>> {
    val result = ttsService.generateAudioWithSync(text)
    return ResponseEntity.ok(result)
  }
}
