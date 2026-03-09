package sv.com.clip.audio.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.k2fsa.sherpa.onnx.*
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.*
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory

import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.client.toEntity
import sv.com.clip.text.api.TextProcessorExternal


import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.encode.AudioAttributes
import ws.schild.jave.encode.EncodingAttributes
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.http.HttpClient
import java.time.Duration

@Service
class TTSService(
  private val recognizerService: RecognizerService,
  private val textProcessor: TextProcessorExternal,
  private val audioService: AudioService,
) {
  private val semaphore = Semaphore(3)
  private val tts: OfflineTts
  // Configuramos el cliente una sola vez (Bean o init)
  private val restClient = RestClient.builder()
    .requestFactory(JdkClientHttpRequestFactory(HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build()).apply { setReadTimeout(Duration.ofSeconds(60)) })
    .baseUrl("http://localhost:8880")
    .build()

  init {
    val modelResource = ClassPathResource("models/en_US-amy-low.onnx").file.absolutePath
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
    tts = OfflineTts(mainConfig)
  }

  private fun fetchFullJsonResponse(sentence: String, voice: String): Map<String, Any>? {
    val kokoroRequest = mapOf(
      "input" to sentence,
      "voice" to voice,
      "model" to "kokoro",
      "stream" to true,
      "response_format" to "mp3" // O "wav" según prefieras
    )

    return try {
      // Obtenemos el JSON completo que incluye "audio" y "tokens"/"timestamps"
      restClient.post()
        .uri("/dev/captioned_speech")
        .contentType(MediaType.APPLICATION_JSON)
        .body(kokoroRequest)
        .retrieve()
        .body<Map<String, Any>>()
    } catch (e: Exception) {
      null
    }
  }

  fun streamTextSincrono(fullText: String, voice: String, outputStream: OutputStream) {
    val sentences = textProcessor.splitBySentence(fullText).filter { it.isNotBlank() }
    val objectMapper = ObjectMapper()

    sentences.forEach { sentence ->
      try {
        // Una por una, aprovechando toda la CPU para cada frase
        val jsonMap = fetchFullJsonResponse(sentence.trim(), voice)

        if (jsonMap != null) {
          val jsonLine = objectMapper.writeValueAsString(jsonMap)
          outputStream.write(jsonLine.toByteArray())
          outputStream.write("\n".toByteArray())
          outputStream.flush() // Enviamos al cliente inmediatamente
        }
      } catch (e: Exception) {
        println("Error en frase: $sentence - ${e.message}")
      }
    }
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



  fun generateMp3(text: String, voice: String = "af_heart"): ByteArray {

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

      // Leemos todo el inputStream y lo convertimos a ByteArray
      val responseBytes = response.body?.inputStream?.readAllBytes() ?: ByteArray(0)

      return responseBytes
    } catch (e: Exception) {
      println("Error generando WAV: ${e.message}")
      ByteArray(0)
    }
  }

  fun generateSpeech(text: String, voice: String = "af_heart"): ByteArray? {

    val requestBody = mapOf(
      "input" to text,
      "voice" to voice,
      "model" to "tts-1",
      "response_format" to "wav"
    )

    return restClient.post()
      .uri("/audio/speech")
      .body(requestBody)
      .retrieve()
      .body<ByteArray>() // Recibes los bytes del audio
  }

  // kokoro_local
  fun generateAudioWithSync(text: String): Map<String, Any> {

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
  }



}

