package sv.com.clip.speech.internal

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import sv.com.clip.speech.api.SpeechAudioExternal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@Component
class SpeechService(
  @Value("\${chatterbox.url:http://localhost:5123/v1/audio/speech}")  private val apiUrl: String,
//  private val httpClient: HttpClient = HttpClient.newHttpClient()
) : SpeechAudioExternal {
  // Force HTTP_1_1 to avoid "Unsupported upgrade request"
  private val httpClient: HttpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)
    .build()
  override fun getSpeechAudio(rawText: String): ByteArray {

// Use a compact string instead of trimIndent() to ensure no hidden chars
    val body = "{\"input\":\"${rawText.replace("\"", "\\\"")}\",\"voice\":\"default\",\"model\":\"tts-1\",\"response_format\":\"wav\"}"

    println("DEBUG - Sending Compact JSON: $body")

    val request = HttpRequest.newBuilder()
      .uri(URI.create(apiUrl))
      .header("Content-Type", "application/json")
      .header("Accept", "audio/wav") // Hint to the server what you expect
      .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)) // Explicit Encoding
      .build()


    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
    return if (response.statusCode() == 200) response.body()
    else throw RuntimeException("TTS Error: ${response.statusCode()}")
  }

//  override fun getSpeechAudio(rawText: String): ByteArray {
//    val apiUrl = "http://localhost:5123/v1/audio/speech"
//
//    // Cuerpo de la petición compatible con OpenAI/Chatterbox
//    val jsonPayload = """
//            {
//                "input": "$rawText",
//                "voice": "default",
//                "model": "tts-1"
//            }
//
//            """.trimIndent()
//
//    val client: HttpClient = HttpClient.newHttpClient()
//    val request: HttpRequest? = HttpRequest.newBuilder()
//      .uri(URI.create(apiUrl))
//      .header("Content-Type", "application/json")
//      .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
//      .build()
//
//
//    // Enviar y guardar el audio directamente en un archivo .wav
//    val outputPath: Path = Paths.get("salida_audio.wav")
//    val response: HttpResponse<Path?> = client.send(
//      request,
//      HttpResponse.BodyHandlers.ofFile(outputPath)
//    )
//
//    if (response.statusCode() === 200) {
//      println("Audio guardado en: " + outputPath.toAbsolutePath())
//    } else {
//      System.err.println("Error: " + response.statusCode())
//    }
//  }
}
