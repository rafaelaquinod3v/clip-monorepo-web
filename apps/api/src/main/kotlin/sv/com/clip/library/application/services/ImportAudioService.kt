package sv.com.clip.library.application.services

import com.k2fsa.sherpa.onnx.OfflineRecognizerResult
import org.springframework.stereotype.Service
import sv.com.clip.audio.internal.RecognizerService
import sv.com.clip.storage.api.StorageApi
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.encode.AudioAttributes
import ws.schild.jave.encode.EncodingAttributes
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Service
class ImportAudioService(
  private val storageApi: StorageApi,
  private val recognizerService: RecognizerService,
) {

  fun processAudioGenerateSRT(fileName: String) {
    val audioBytes = storageApi.load(fileName) ?: throw RuntimeException("No se pudo leer el archivo: $fileName")

    // 2. Convertir Bytes a FloatArray (PCM 16-bit)
    // Sherpa-ONNX requiere FloatArray normalizado [-1.0, 1.0]
  //val audioFloats = decodeToFloatArray(audioBytes)
    val audioFloats = decodeMp3To24kHzMono(audioBytes)

    // 3. Obtener resultado del modelo (usa 16000 o 24000 según tu config)
    val result = recognizerService.getTimestampsFromAudio(audioFloats, 24000f)

    // 4. Formatear a SRT
    val srt = formatToSRT(result)
    println("Srt: $srt")
    // 4. Guardar en disco
    // Cambiamos la extensión del nombre original a .srt
    //val srtFileName = fileName.substringBeforeLast(".") + ".srt"
    val savedName = storageApi.store(srt.toByteArray(Charsets.UTF_8))

    println("Subtítulo guardado como: $savedName")
  }

  private fun decodeToFloatArray(audioBytes: ByteArray): FloatArray {
    // Si es un WAV simple, puedes extraer los bytes saltando el header (44 bytes)
    // Pero lo ideal es usar una librería como JAVE2 o FFMPEG-Java
    // para asegurar que el sample rate sea el correcto (24kHz en tu caso).

    // Ejemplo simplificado para PCM de 16 bits:
    val shortBuffer = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
    val floatArray = FloatArray(shortBuffer.capacity())
    for (i in 0 until shortBuffer.capacity()) {
      floatArray[i] = shortBuffer.get(i) / 32768.0f // Normalización
    }
    return floatArray
  }

  private fun decodeMp3To24kHzMono(mp3Bytes: ByteArray): FloatArray {
    // JAVE2/FFmpeg se encarga de convertir cualquier sample rate de entrada a 24000Hz
    val source = File.createTempFile("input", ".mp3").apply { writeBytes(mp3Bytes) }
    val target = File.createTempFile("output", ".wav")

    val audioAttributes = AudioAttributes().apply {
      setCodec("pcm_s16le")
      setSamplingRate(24000)
      setChannels(1)
    }
    val encodingAttributes = EncodingAttributes().apply {
      setAudioAttributes(audioAttributes)
      setOutputFormat("wav")
    }

    Encoder().encode(MultimediaObject(source), target, encodingAttributes)

    // Leer el WAV resultante y convertir a FloatArray normalizado
    val wavBytes = target.readBytes()
    val shortBuffer = ByteBuffer.wrap(wavBytes)
      .order(ByteOrder.LITTLE_ENDIAN)
      .position(44) // Saltar el header del WAV
      .asShortBuffer()

    return FloatArray(shortBuffer.remaining()) { shortBuffer.get() / 32768.0f }
  }

  private fun formatToSRT(result: OfflineRecognizerResult): String {
    val sb = StringBuilder()
    val tokens = result.tokens
    val timestamps = result.timestamps

    if (tokens.isEmpty()) return ""

    // Agrupación simple: 8 tokens por segmento de subtítulo
    tokens.toList().chunked(8).zip(timestamps.toList().chunked(8)).forEachIndexed { index, (chunkTokens, chunkTimes) ->
      val startTime = chunkTimes.first()
      val endTime = chunkTimes.last() + 0.5f // Añadir margen
      val text = chunkTokens.joinToString("").replace(" ", " ").trim()

      sb.append("${index + 1}\n")
      sb.append("${formatTime(startTime)} --> ${formatTime(endTime)}\n")
      sb.append("$text\n\n")
    }
    return sb.toString()
  }

  private fun formatTime(seconds: Float): String {
    val totalMs = (seconds * 1000).toLong()
    val h = totalMs / 3600000
    val m = (totalMs % 3600000) / 60000
    val s = (totalMs % 60000) / 1000
    val ms = totalMs % 1000
    return "%02d:%02d:%02d,%03d".format(h, m, s, ms)
  }


}
