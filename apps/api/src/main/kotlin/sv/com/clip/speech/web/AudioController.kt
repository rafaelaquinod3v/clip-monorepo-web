package sv.com.clip.speech.web

import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.*
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import sv.com.clip.speech.internal.TtsService
import java.net.http.HttpClient
import java.time.Duration

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
    val result = ttsService.generateAudioWithSyncV2(text)
    return ResponseEntity.ok(result)
  }
  @PostMapping("/stream-book")
  fun streamSpeech(@RequestBody request: Map<String, String>): ResponseEntity<StreamingResponseBody> {
    // Configuramos la petición a Kokoro-FastAPI
    val kokoroRequest = mapOf(
      "input" to request["text"],
      "voice" to (request["voice"] ?: "af_heart"),
      "model" to "kokoro",
      "stream" to true,           // Activamos streaming nativo
      "response_format" to "mp3",
      "return_timestamps" to true,
      "return_download_link" to false,
    )
    val nativeClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10)) // Aquí se pone el CONNECT timeout
      .build()
    val factory = JdkClientHttpRequestFactory(nativeClient)
    factory.setReadTimeout(Duration.ofSeconds(30)) // This is the Response Timeout


    val restClient = RestClient.builder()
      .requestFactory(factory)
      .baseUrl("http://localhost:8880")
      .build()

    val responseBody = StreamingResponseBody { outputStream ->
      restClient.post()
        .uri("/dev/captioned_speech")
       // .header("Accept", "application/x-ndjson")
        .body(kokoroRequest)
        .retrieve()
        .onStatus({ it.isError }) { _, res -> throw RuntimeException("Error en Kokoro: ${res.statusCode}") }
        .toEntity<Resource>()
        .body?.inputStream?.use { inputStream ->
          // Copiamos el stream de Kokoro directamente al cliente (frontend)
          inputStream.transferTo(outputStream)
        }
    }

    return ResponseEntity.ok()
      .contentType(MediaType.APPLICATION_OCTET_STREAM) // O un MIME type específico si usas JSON streaming
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
