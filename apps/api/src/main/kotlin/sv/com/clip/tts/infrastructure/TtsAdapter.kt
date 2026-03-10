package sv.com.clip.tts.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.*
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import sv.com.clip.text.api.TextProcessorExternal
import sv.com.clip.tts.domain.TtsPort
import sv.com.clip.tts.infrastructure.config.TtsProperties
import java.io.OutputStream
import java.net.http.HttpClient
import java.time.Duration
import java.util.Base64

@Component
class TtsAdapter(
  private val props: TtsProperties,
  private val textProcessor: TextProcessorExternal,
  private val objectMapper: ObjectMapper,
) : TtsPort {

  // Configuramos el cliente una sola vez (Bean o init)
  private val restClient = RestClient.builder()
          .requestFactory(JdkClientHttpRequestFactory(HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(props.connectTimeoutSeconds))
          .build()).apply { setReadTimeout(Duration.ofSeconds(props.readTimeoutSeconds)) })
          .baseUrl(props.baseUrl)
          .build()

  override fun textSplitBySentenceSync(
    fullText: String,
    voice: String,
    outputStream: OutputStream
  ) {
    val sentences = textProcessor.splitBySentence(fullText)
    val validSentences = sentences.filter { it.isNotBlank() }

    validSentences.forEachIndexed { _, sentence ->
      val kokoroRequest = mapOf(
        "model" to "tts-1",
        "input" to sentence.trim(),
        "voice" to voice,
        "response_format" to "mp3"
      )

      try {
        val audioBytes = restClient.post()
          .uri("/v1/audio/speech")
          .contentType(MediaType.APPLICATION_JSON)
          .body(kokoroRequest)
          .retrieve()
          .body(ByteArray::class.java)

        audioBytes?.let {
          val chunk = mapOf(
            "audio" to Base64.getEncoder().encodeToString(it),
            "timestamps" to emptyList<Any>()
          )
          val line = objectMapper.writeValueAsString(chunk) + "\n"
          outputStream.write(line.toByteArray())
          outputStream.flush()
        }
      } catch (e: Exception) {
        println("Error procesando sentencia: $sentence - ${e.message}")
      }
    }
  }

}

