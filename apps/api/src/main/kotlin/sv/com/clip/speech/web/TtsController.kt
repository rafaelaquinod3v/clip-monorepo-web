package sv.com.clip.speech.web

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import sv.com.clip.speech.internal.SpeechService

@RestController
@RequestMapping("/api/tts")
class TtsController(private val speechService: SpeechService) {
  @GetMapping("/stream", produces = ["audio/wav"])
  fun streamAudio(@RequestParam text: String): ResponseEntity<ByteArray> {
    val audioData = speechService.getSpeechAudio(text)

    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.wav\"")
      .header(HttpHeaders.CONTENT_TYPE, "audio/wav")
      .body(audioData)
  }
}
