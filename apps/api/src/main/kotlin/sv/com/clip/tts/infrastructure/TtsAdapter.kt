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
import java.io.OutputStream
import java.net.http.HttpClient
import java.time.Duration
import java.util.Base64

@Component
class TtsAdapter(
  //private val recognizerService: RecognizerService,
  private val textProcessor: TextProcessorExternal,
  private val objectMapper: ObjectMapper,
 // private val audioService: AudioService,
) : TtsPort {
  //private val tts: OfflineTts
  // Configuramos el cliente una sola vez (Bean o init)
  private val restClient = RestClient.builder()
    .requestFactory(JdkClientHttpRequestFactory(HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build()).apply { setReadTimeout(Duration.ofSeconds(60)) })
    .baseUrl("http://localhost:8880")
    .build()

  init {
/*    val modelResource = ClassPathResource("models/en_US-amy-low.onnx").file.absolutePath
    val tokensResource = ClassPathResource("models/tokens.txt").file.absolutePath
    val dataDirResource = ClassPathResource("models/espeak-ng-data").file.absolutePath
    // 1. Build VITS Config
    val vitsConfig = OfflineTtsVitsModelConfig.builder()
      .setModel(modelResource)
      .setTokens(tokensResource)
      .setDataDir(dataDirResource)
      .setNoiseScale(0.667f)
      .setNoiseScaleW(0.8f)
      .setLengthScale(1f)
      .build()

    // 2. Build Model Config (Includes VITS and threading)
    val modelConfig = OfflineTtsModelConfig.builder()
      .setVits(vitsConfig)
      .setNumThreads(4)
      .setDebug(false)
      .setProvider("cpu")
      .build()

    // 3. Build Main TTS Config
    val mainConfig = OfflineTtsConfig.builder()
      .setModel(modelConfig)
      .build()

    // 4. Instantiate the engine
    tts = OfflineTts(mainConfig)*/
  }


  fun streamTextSplitBySentenceSync(fullText: String, voice: String, outputStream: OutputStream) {
    val sentences = textProcessor.splitBySentence(fullText)

    sentences.forEach { sentence ->
      println(sentence)
      if (sentence.isBlank()) return@forEach

      val kokoroRequest = mapOf(
        "input" to sentence.trim(),
        "voice" to voice,
        "model" to "kokoro",
        "stream" to true,
        "response_format" to "mp3"
      )

      try {
        val responseEntity = restClient.post()
          .uri("/dev/captioned_speech")
          .contentType(MediaType.APPLICATION_JSON)
          .body(kokoroRequest)
          .retrieve()
          .toEntity<Resource>()

          responseEntity.body?.inputStream?.use { inputStream ->
            inputStream.copyTo(outputStream)
            outputStream.write("\n".toByteArray())
            outputStream.flush()
          }
      } catch (e: Exception) {
        println("Error procesando sentencia: $sentence - ${e.message}")
      }
    }
  }
  fun streamTextSplitBySentenceAsync(
    fullText: String,
    voice: String,
    outputStream: OutputStream,
    httpRequest: HttpServletRequest
  ) {
    val sentences = textProcessor.splitBySentence(fullText)
      .filter { it.isNotBlank() }
      .map { it.trim() }

    val job = Job()

    // Spring notifica cuando el cliente se desconecta
    httpRequest.asyncContext?.addListener(object : AsyncListener {
      override fun onComplete(event: AsyncEvent) = Unit
      override fun onStartAsync(event: AsyncEvent) = Unit
      override fun onTimeout(event: AsyncEvent) { job.cancel() }
      override fun onError(event: AsyncEvent) { job.cancel() }
    })

    runBlocking(job) {
      try {
        val deferredResults = sentences.mapIndexed { index, sentence ->
          async(Dispatchers.IO) {
            ensureActive() // ← cancela si el job fue cancelado
            println(sentence)
            val kokoroRequest = mapOf(
              "input" to sentence,
              "voice" to voice,
              "model" to "kokoro",
              "stream" to true,
              "response_format" to "mp3"
            )
            try {
              val responseEntity = restClient.post()
                .uri("/dev/captioned_speech")
                .contentType(MediaType.APPLICATION_JSON)
                .body(kokoroRequest)
                .retrieve()
                .toEntity<Resource>()

              index to responseEntity.body?.inputStream?.readBytes()
            } catch (e: Exception) {
              if (e is CancellationException) throw e // propagar cancelación
              println("Error procesando sentencia: $sentence - ${e.message}")
              index to null
            }
          }
        }

        deferredResults
          .awaitAll()
          .sortedBy { it.first }
          .forEach { (_, bytes) ->
            ensureActive() // ← verificar antes de cada escritura
            if (bytes != null) {
              outputStream.write(bytes)
              outputStream.write("\n".toByteArray())
              outputStream.flush()
            }
          }
      } catch (e: CancellationException) {
        println("Stream cancelado por desconexión del cliente")
      } finally {
        job.cancel() // asegurarse de limpiar
      }
    }
  }

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

/*  override fun textSplitBySentenceSync(
    fullText: String,
    voice: String,
    outputStream: OutputStream
  ) {
    val sentences = textProcessor.splitBySentence(fullText)

    sentences.forEach { sentence ->
      if (sentence.isBlank()) return@forEach

      val kokoroRequest = mapOf(
        "model" to "tts-1",
        "input" to sentence.trim(),
        "voice" to voice,
        "response_format" to "mp3"
        // sin "stream" — Kokoros devuelve el MP3 completo de la oración
      )

      try {
        val audioBytes = restClient.post()
          .uri("/v1/audio/speech")
          .contentType(MediaType.APPLICATION_JSON)
          .body(kokoroRequest)
          .retrieve()
          .body(ByteArray::class.java)

        audioBytes?.let {
          outputStream.write(it)
          outputStream.flush()
        }
      } catch (e: Exception) {
        println("Error procesando sentencia: $sentence - ${e.message}")
      }
    }
  }*/

/*  fun generateMp3(text: String, voice: String = "af_heart"): ByteArray {

    val kokoroRequest = mapOf(
      "input" to text.trim(),
      "voice" to voice,
      "model" to "kokoro",
      "stream" to false,
      "response_format" to "mp3"
    )

    return try {
      val response = restClient.post()
        .uri("/v1/audio/speech")
        .contentType(MediaType.APPLICATION_JSON)
        .body(kokoroRequest)
        .retrieve()
        .toEntity<Resource>()


      val responseBytes = response.body?.inputStream?.readAllBytes() ?: ByteArray(0)

      return responseBytes
    } catch (e: Exception) {
      println("Error generando WAV: ${e.message}")
      ByteArray(0)
    }
  }*/

  // kokoro_local
/*  fun generateAudioWithSync(text: String): Map<String, Any> {

    val samples = generateSpeech(text) //audio.samples
    val sampleRate = 24000 ///audio.sampleRate

    // 2. Recognize (Pass 22050 so Sherpa calculates seconds correctly)
    val floatArrayData = audioService.byteToFloatArrayWav(samples!!)
    val totalDuration = floatArrayData.size.toDouble() / sampleRate
    val result = recognizerService.getTimestampsFromAudio(floatArrayData, sampleRate.toFloat())
    // 3. Group sub-tokens into Words
    val wordAlignments = mutableListOf<Map<String, Any>>()
    // var currentWord: MutableMap<String, Any>? = null
    var currentWord: MutableMap<String, Any>? = null
    var wordCounter = 0 // This corresponds to the index in a split(" ") array

    result.tokens.forEachIndexed { i, token ->
      val punctuationRegex = Regex("[.,!?;:]")
      // Whisper marca el inicio de palabra con " " (espacio de BPE)
      val isNewWord = token.startsWith(" ") || token.startsWith("_")
      //val cleanToken = token.replace(" ", "").replace("_", "")
      val hasSpace = token.startsWith(" ") || token.startsWith("_")
      val cleanToken = token.replace(Regex("[ _]"), "")
      val isContinuation = !hasSpace || cleanToken.matches(punctuationRegex)
      //val cleanToken = token.replace(Regex("[ _]"), "")
      val nextTokenStart = result.timestamps.getOrElse(i + 1) { totalDuration.toFloat() }
      if (!isContinuation || currentWord == null) {
        // If it's a new word, save the previous one and start a new one
        currentWord?.let {
          wordAlignments.add(it)
          wordCounter += 2 // frontend text.split(/(\s+)/)
        }

        currentWord = mutableMapOf(
          "term" to cleanToken,
          "start" to result.timestamps[i],
          "end" to nextTokenStart,
          "index" to wordCounter
        )
      } else {
        // It's a sub-token (like "ll" or "o"), append to the current word
        currentWord["term"] = currentWord["term"].toString() + cleanToken
        currentWord["end"] = nextTokenStart
      }
    }
    // Add the last word
    currentWord?.let { wordAlignments.add(it) }

    return mapOf(
      "sampleRate" to sampleRate,
      "duration" to totalDuration,
      "audio" to Base64.getEncoder().encodeToString(audioService.convertWavToMp3(samples)),
      "alignment" to wordAlignments
    )
  }*/



}

