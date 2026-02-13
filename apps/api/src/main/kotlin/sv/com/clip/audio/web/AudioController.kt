package sv.com.clip.audio.web

import org.springframework.core.io.ByteArrayResource
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import sv.com.clip.audio.internal.TTSService

@RestController
@RequestMapping("/api/audio")
class AudioController(private val ttsService: TTSService,) {

  @GetMapping("/speak", produces = ["audio/mpeg"])
  fun speak(@RequestParam text: String): ResponseEntity<ByteArray> {
    val wavData = ttsService.generateWav(text)

    val headers = HttpHeaders()
    headers.contentType = MediaType.parseMediaType("audio/mpeg")
    headers.contentDisposition = ContentDisposition.inline().filename("speech.mp3").build()

    return ResponseEntity(wavData, headers, HttpStatus.OK)
  }

  @GetMapping("/synthesize")
  fun synthesize(@RequestParam text: String): ResponseEntity<Map<String, Any>> {
    val result = ttsService.generateAudioWithSyncV2(text)
    return ResponseEntity.ok(result)
  }

  @PostMapping("/stream-book")
  fun streamBook(@RequestBody request: Map<String, String>): ResponseEntity<StreamingResponseBody> {
    val fullText = request["text"] ?: throw IllegalArgumentException("Text is required")
    val voice = request["voice"] ?: "af_heart"

    val responseBody = StreamingResponseBody { outputStream ->
      // Delegamos TODA la lógica al servicio
      ttsService.streamTextSplitBySentence(fullText, voice, outputStream)
    }

    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("application/x-ndjson"))
      .body(responseBody)
  }

  @GetMapping("/download-audio", produces = [MediaType.MULTIPART_FORM_DATA_VALUE])
  fun downloadAudio(@RequestParam text: String): ResponseEntity<MultiValueMap<String, Any>> {

    val formData: MultiValueMap<String, Any> = LinkedMultiValueMap()

    // 1. Parte 1: Metadatos en formato JSON
    val metadataHeaders = HttpHeaders().apply {
      contentType = MediaType.APPLICATION_JSON
    }
    val metadataPart = HttpEntity("{\"status\": \"success\", \"duration\": 120}", metadataHeaders)
    formData.add("metadata", metadataPart)

    val wavData = ttsService.generateWav(text)
    // 2. Parte 2: El archivo de audio binario
    val audioBytes = wavData // Aquí obtienes tus bytes reales del audio
    val audioHeaders = HttpHeaders().apply {
      contentType = MediaType.parseMediaType("audio/wav")
      // El navegador/cliente lo identificará como un archivo adjunto
      setContentDispositionFormData("audioFile", "audio.wav")
    }
    val audioPart = HttpEntity(ByteArrayResource(audioBytes), audioHeaders)
    formData.add("audioFile", audioPart)

    return ResponseEntity(formData, HttpStatus.OK)
  }

  @GetMapping("/generate-speech", produces = ["audio/mp3"])
  fun generateSpeech(@RequestParam text: String): ResponseEntity<ByteArray> {
    val mp3Data = ttsService.generateSpeech(text)

    val headers = HttpHeaders()
    headers.contentType = MediaType.parseMediaType("audio/mp3")
    headers.contentDisposition = ContentDisposition.inline().filename("speech.mp3").build()

    return ResponseEntity(mp3Data, headers, HttpStatus.OK)
  }


}
