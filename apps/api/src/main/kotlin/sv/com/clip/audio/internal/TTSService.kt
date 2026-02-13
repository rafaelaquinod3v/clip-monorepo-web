package sv.com.clip.audio.internal

import com.k2fsa.sherpa.onnx.*

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory

import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.client.toEntity
import sv.com.clip.text.api.TextProcessorExternal


import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.encode.AudioAttributes
import ws.schild.jave.encode.EncodingAttributes
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.http.HttpClient
import java.time.Duration

@Service
class TTSService(
  private val recognizerService: RecognizerService,
  private val textProcessor: TextProcessorExternal,
) {

  private val tts: OfflineTts
  // Sample Rate nativo fijo de 22050 Hz
  //private val sampleRate = 22050.0f // en_US-amy-low.onnx

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

/*
  fun generateWav(text: String): ByteArray {
    val audio = tts.generate(text)

    val samples = audio.samples // This is a FloatArray

    if (samples.isEmpty()) return ByteArray(0)

    return convertSamplesToWav(samples, audio.sampleRate.toFloat())
  }
*/

  private fun writeWavHeader(out: ByteArrayOutputStream, numSamples: Int, sampleRate: Float) {
    val channels = 1
    val byteRate = sampleRate * channels * 2
    val blockAlign = channels * 2
    val dataSize = numSamples * channels * 2
    val chunkSize = 36 + dataSize

    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray())
    header.putInt(chunkSize)
    header.put("WAVE".toByteArray())
    header.put("fmt ".toByteArray())
    header.putInt(16) // Subchunk1Size
    header.putShort(1.toShort()) // AudioFormat (PCM)
    header.putShort(channels.toShort())
    header.putInt(sampleRate.toInt())
    header.putInt(byteRate.toInt())
    header.putShort(blockAlign.toShort())
    header.putShort(16.toShort()) // BitsPerSample
    header.put("data".toByteArray())
    header.putInt(dataSize)

    out.write(header.array())
  }
  fun generateAudioWithSync(text: String): Map<String, Any> {
    val audio = tts.generate(text)
    val samples = audio.samples
    val sampleRate = audio.sampleRate

    // 1. Padding: Add silence at the BEGINNING
   // val paddingSize = 8000
    //val paddedSamples = FloatArray(samples.size + paddingSize)
    //samples.copyInto(paddedSamples, paddingSize) // Move audio to start after 8000 samples
    val totalDuration = samples.size.toDouble() / sampleRate
    // 2. Recognize (Pass 22050 so Sherpa calculates seconds correctly)
    val result = recognizerService.getTimestampsFromAudio(samples, sampleRate.toFloat())
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

    //wordAlignments.forEach { println(it["term"]) }

    return mapOf(
      "sampleRate" to sampleRate,
      "duration" to totalDuration,
      "audio" to Base64.getEncoder().encodeToString(convertWavToMp3(convertSamplesToWav(samples, sampleRate.toFloat()))),
      "alignment" to wordAlignments
    )
  }

  // Refactor your existing generateWav logic into a helper that takes samples
  private fun convertSamplesToWav(samples: FloatArray, sampleRate: Float): ByteArray {
    val out = ByteArrayOutputStream()
    writeWavHeader(out, samples.size, sampleRate)
    val buffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
    for (sample in samples) {
      val s = (sample * 32767).toInt().coerceIn(-32768, 32767)
      buffer.putShort(s.toShort())
    }
    out.write(buffer.array())
    return out.toByteArray()
  }

  fun convertWavToMp3(wavBytes: ByteArray): ByteArray {
    // JAVE2 a veces requiere un archivo temporal porque FFmpeg lo prefiere
    val tempWav = File.createTempFile("temp_audio", ".wav")
    val tempMp3 = File.createTempFile("temp_audio", ".mp3")

    try {
      tempWav.writeBytes(wavBytes)

      // Configuración de Audio
      val audio = AudioAttributes().apply {
        setCodec("libmp3lame")
        setBitRate(64000) // 64kbps es ideal para voz
        setChannels(1)
        setSamplingRate(16000) // Mantener tus 16k
      }

      // Configuración de Codificación
      val attrs = EncodingAttributes().apply {
        setOutputFormat("mp3")
        setAudioAttributes(audio)
      }

      // Ejecutar conversión
      Encoder().encode(MultimediaObject(tempWav), tempMp3, attrs)

      return tempMp3.readBytes()
    } finally {
      // Limpieza de archivos temporales
      tempWav.delete()
      tempMp3.delete()
    }
  }
  // Configuramos el cliente una sola vez (Bean o init)
  private val restClient = RestClient.builder()
    .requestFactory(JdkClientHttpRequestFactory(HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build()).apply { setReadTimeout(Duration.ofSeconds(60)) })
    .baseUrl("http://localhost:8880")
    .build()

  fun streamTextSplitBySentence(fullText: String, voice: String, outputStream: OutputStream) {
    val sentences = textProcessor.splitBySentence(fullText)

    sentences.forEach { sentence ->
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

  fun generateWav(text: String, voice: String = "af_heart"): ByteArray {

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
      //response.body?.inputStream?.use { convertSamplesToWav(it.readAllBytes(), 24000f) } ?: ByteArray(0)
      val responseBytes = response.body?.inputStream?.readAllBytes() ?: ByteArray(0)

      val sampleRate = 24000.0f
      val durationSeconds = responseBytes.size.toDouble() / (sampleRate * 4) // Asumiendo 32-bit (4 bytes)
      val durationSeconds16 = responseBytes.size.toDouble() / (sampleRate * 2) // Asumiendo 16-bit (2 bytes)

      println("--- DEBUG AUDIO ---")
      println("Tamaño total recibido: ${responseBytes.size} bytes")
      println("Si fuera 32-bit, duraría: $durationSeconds segundos")
      println("Si fuera 16-bit, duraría: $durationSeconds16 segundos")
      println("-------------------")
      // --- DEBUG DE BYTES ---
      val hexString = responseBytes.take(20).joinToString(" ") { "%02x".format(it) }
      val asText = String(responseBytes.take(20).toByteArray())
      println("Primeros 20 bytes (Hex): $hexString")
      println("Primeros 20 bytes (Texto): $asText")
      return responseBytes
    } catch (e: Exception) {
      println("Error generando WAV: ${e.message}")
      ByteArray(0)
    }
  }

  fun generateSpeech(text: String, voice: String = "af_heart"): ByteArray? {

    val nativeClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10)) // Aquí se pone el CONNECT timeout
      .build()
    val factory = JdkClientHttpRequestFactory(nativeClient)
    factory.setReadTimeout(Duration.ofSeconds(30)) // This is the Response Timeout


    val restClient = RestClient.builder()
      .requestFactory(factory)
      .baseUrl("http://localhost:8880/v1")
      .build()

    val requestBody = mapOf(
      "input" to text,
      "voice" to voice,
      "model" to "tts-1", // Requerido por compatibilidad OpenAI
      //"response_format" to "mp3"
      "response_format" to "wav"
    )

    return restClient.post()
      .uri("/audio/speech")
      .body(requestBody)
      .retrieve()
      .body<ByteArray>() // Recibes los bytes del audio
  }

  fun generateSpeechStreaming(text: String, voice: String = "af_heart"): InputStream? {

    val nativeClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10)) // Aquí se pone el CONNECT timeout
      .build()
    val factory = JdkClientHttpRequestFactory(nativeClient)
    factory.setReadTimeout(Duration.ofSeconds(30)) // This is the Response Timeout


    val restClient = RestClient.builder()
      .requestFactory(factory)
      .baseUrl("http://localhost:8880/v1")
      .build()

    val requestBody = mapOf(
      "input" to text,
      "voice" to voice,
      "model" to "kokoro", // Nombre del modelo específico
      "response_format" to "opus", // Opus es mejor para streaming que WAV
      "stream" to true,
      "output_format" to "json" // O el parámetro específico que habilite timestamps en tu versión
    )

    return restClient.post()
      .uri("/audio/speech")
      .body(requestBody)
      .retrieve()
      .body<Resource>()?.inputStream // Recibes los bytes del audio
  }

  fun generateAudioWithSyncV2(text: String): Map<String, Any> {

    val samples = generateSpeech(text) //audio.samples
    val sampleRate = 24000 ///audio.sampleRate

    // 2. Recognize (Pass 22050 so Sherpa calculates seconds correctly)
    val floatArrayData = byteToFloatArrayWav(samples!!)
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

    //wordAlignments.forEach { println(it["term"]) }

    return mapOf(
      "sampleRate" to sampleRate,
      "duration" to totalDuration,
      "audio" to Base64.getEncoder().encodeToString(convertWavToMp3(samples)),
      "alignment" to wordAlignments
    )
  }


  fun byteToFloatArrayWav(audioBytes: ByteArray): FloatArray {
    val headerSize = 44 // Cabecera estándar de WAV
    val pcmBytes = audioBytes.copyOfRange(headerSize, audioBytes.size)

    val n = pcmBytes.size / 2
    val floatAudio = FloatArray(n)
    val buffer = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)

    for (i in 0 until n) {
      floatAudio[i] = buffer.short.toFloat() / 32768.0f
    }
    return floatAudio
  }

  fun convertSamplesToWav(pcmBytes: ByteArray, sampleRate: Float): ByteArray {
    val headerSize = 44
    val totalDataLen = pcmBytes.size + headerSize - 8
    // ByteRate para 16-bit: SampleRate * 1 canal * 2 bytes
    val byteRate = (sampleRate * 1 * 2).toLong()

    val wavBuffer = ByteBuffer.allocate(headerSize + pcmBytes.size)
    wavBuffer.order(ByteOrder.LITTLE_ENDIAN)

    // RIFF
    wavBuffer.put("RIFF".toByteArray())
    wavBuffer.putInt(totalDataLen)
    wavBuffer.put("WAVE".toByteArray())

    // FMT
    wavBuffer.put("fmt ".toByteArray())
    wavBuffer.putInt(16)            // Subchunk1Size
    wavBuffer.putShort(1.toShort())  // AudioFormat: 1 = PCM (IMPORTANTE: No usar 3)
    wavBuffer.putShort(1.toShort())  // Channels: Mono
    wavBuffer.putInt(sampleRate.toInt())
    wavBuffer.putInt(byteRate.toInt())
    wavBuffer.putShort(2.toShort())  // BlockAlign: 1 canal * 2 bytes
    wavBuffer.putShort(16.toShort()) // BitsPerSample: 16

    // DATA
    wavBuffer.put("data".toByteArray())
    wavBuffer.putInt(pcmBytes.size)
    wavBuffer.put(pcmBytes)

    return wavBuffer.array()
  }

}

