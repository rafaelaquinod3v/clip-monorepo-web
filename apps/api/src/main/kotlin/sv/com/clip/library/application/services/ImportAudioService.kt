package sv.com.clip.library.application.services

import org.springframework.stereotype.Service
import sv.com.clip.asr.api.AsrProvider
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
  private val asrProvider: AsrProvider,
) {
  private data class Word(val text: String, val startTime: Float)
/*  fun processAudioGenerateSRT(fileName: String) {
    val audioBytes = storageApi.load(fileName)
      ?: throw RuntimeException("No se pudo leer el archivo: $fileName")

    // Guardar temporalmente para procesarlo con JAVE/FFmpeg
    val tempInput = File.createTempFile("input_full", ".mp3")
      .apply { writeBytes(audioBytes) }

    // Dividir en chunks por silencios
    val silencePoints = detectSilencePoints(tempInput.absolutePath)
    val cutPoints = calculateCutPoints(silencePoints, maxChunkSeconds = 60)
    val chunks = extractChunks(tempInput.absolutePath, cutPoints)

    // Procesar cada chunk y acumular resultados con offset de tiempo
    val allTokens = mutableListOf<String>()
    val allTimestamps = mutableListOf<Float>()
    var timeOffset = 0f

    chunks.forEach { chunk ->
      val chunkBytes = chunk.readBytes()
      val audioFloats = decodeMp3To24kHzMono(chunkBytes)
      val result = asrProvider.getTimestampsFromAudio(audioFloats, 24000f)

      // Agregar offset de tiempo para que los timestamps sean globales
      allTokens.addAll(result.tokens.toList())
      allTimestamps.addAll(result.timestamps.map { it + timeOffset })

      timeOffset += chunk.let {
        MultimediaObject(it).info.duration / 1000f
      }

      chunk.delete() // limpiar chunk temporal
    }

    tempInput.delete() // limpiar archivo original temporal

    // Formatear SRT con todos los tokens y timestamps combinados
*//*    val srt = formatToSRTFromRaw(allTokens, allTimestamps)
    println("Srt: $srt")*//*
    val lrc = formatToLrcKaraoke(
      tokens = allTokens,
      timestamps = allTimestamps,
      title = fileName.substringBeforeLast(".")
    )
    println("Lrc: $lrc")
    val savedName = storageApi.store(lrc.toByteArray(Charsets.UTF_8))
    println("Subtítulo guardado como: $savedName")
  }*/

  // Nuevo formatToSRT que acepta listas directamente
  private fun formatToSRTFromRaw(tokens: List<String>, timestamps: List<Float>): String {
    if (tokens.isEmpty()) return ""
    val sb = StringBuilder()

    tokens.chunked(8).zip(timestamps.chunked(8)).forEachIndexed { index, (chunkTokens, chunkTimes) ->
      val startTime = chunkTimes.first()
      val endTime = chunkTimes.last() + 0.5f
      val text = chunkTokens.joinToString("").trim()

      sb.append("${index + 1}\n")
      sb.append("${formatTime(startTime)} --> ${formatTime(endTime)}\n")
      sb.append("$text\n\n")
    }
    return sb.toString()
  }

  // Detectar silencios con FFmpeg
  private fun detectSilencePoints(inputFile: String, thresholdDb: Int = -30, minDuration: Double = 0.5): List<Double> {
    val command = listOf(
      "ffmpeg", "-i", inputFile,
      "-af", "silencedetect=noise=${thresholdDb}dB:duration=$minDuration",
      "-f", "null", "-"
    )

    val process = ProcessBuilder(command)
      .redirectErrorStream(true)
      .start()

    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()

    return Regex("""silence_end:\s*([\d.]+)""")
      .findAll(output)
      .map { it.groupValues[1].toDouble() }
      .toList()
  }

  // Calcular puntos de corte respetando maxChunkSeconds
  private fun calculateCutPoints(silencePoints: List<Double>, maxChunkSeconds: Int): List<Double> {
    if (silencePoints.isEmpty()) return listOf(0.0)

    val cutPoints = mutableListOf(0.0)
    var lastCut = 0.0

    for (point in silencePoints) {
      if (point - lastCut >= maxChunkSeconds) {
        cutPoints.add(point)
        lastCut = point
      }
    }

    return cutPoints
  }

  /*  fun processAudioGenerateSRT(fileName: String) {
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
    }*/

/*  private fun decodeToFloatArray(audioBytes: ByteArray): FloatArray {
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
  }*/

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

/*  private fun formatToSRT(result: OfflineRecognizerResult): String {
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
  }*/

  private fun formatTime(seconds: Float): String {
    val totalMs = (seconds * 1000).toLong()
    val h = totalMs / 3600000
    val m = (totalMs % 3600000) / 60000
    val s = (totalMs % 60000) / 1000
    val ms = totalMs % 1000
    return "%02d:%02d:%02d,%03d".format(h, m, s, ms)
  }
  private fun extractChunks(inputFile: String, cutPoints: List<Double>): List<File> {
    val outputFiles = mutableListOf<File>()
    val inputFileObj = File(inputFile)
    val outputDir = inputFileObj.parentFile
    val baseName = inputFileObj.nameWithoutExtension
    val multimedia = MultimediaObject(inputFileObj)
    val totalDuration = multimedia.info.duration / 1000.0 // JAVE devuelve ms

    val encoder = Encoder()

    val audioAttributes = AudioAttributes().apply {
      setCodec("libmp3lame")
      setBitRate(128000)
      setChannels(1)      // mono para Whisper
      setSamplingRate(24000) // 24kHz para Whisper
    }

    for (i in cutPoints.indices) {
      val startMs = (cutPoints[i] * 1000).toLong()
      val endMs = ((if (i + 1 < cutPoints.size) cutPoints[i + 1] else totalDuration) * 1000).toLong()
      val durationMs = endMs - startMs

      val outputFile = File(outputDir, "${baseName}_chunk_${i}.mp3")

      val encodingAttributes = EncodingAttributes().apply {
        setInputFormat("mp3")
        setOutputFormat("mp3")
        setOffset(startMs / 1000.0f)  // JAVE usa segundos como Float
        setDuration(durationMs / 1000.0f)
        setAudioAttributes(audioAttributes)
      }

      encoder.encode(multimedia, outputFile, encodingAttributes)

      if (outputFile.exists() && outputFile.length() > 0) {
        outputFiles.add(outputFile)
        println("Chunk creado: ${outputFile.name} [${startMs}ms - ${endMs}ms]")
      }
    }

    return outputFiles
  }

  /**
   * Duración total usando JAVE
   */
/*  private fun getAudioDuration(inputFile: String): Double {
    val multimedia = MultimediaObject(File(inputFile))
    return multimedia.info.duration / 1000.0 // ms a segundos
  }*/
  private fun formatToLrcKaraoke(
    tokens: List<String>,
    timestamps: List<Float>,
    title: String = ""
  ): String {
    if (tokens.isEmpty()) return ""

    // ── 1. Fusionar subword tokens en palabras reales ──────────────────


    val words = mutableListOf<Word>()

/*    tokens.zip(timestamps).forEach { (token, time) ->
      // Sherpa-ONNX usa "▁" (U+2581) para marcar inicio de palabra
      // Whisper usa " " (espacio normal) al inicio del token
      val isNewWord = token.startsWith("▁")
        || token.startsWith(" ")
        || words.isEmpty()

      val clean = token.trimStart('▁', ' ')

      if (clean.isEmpty()) return@forEach // skip tokens vacíos

      if (isNewWord) {
        words.add(Word(text = clean, startTime = time))
      } else {
        // Subword: fusionar con la palabra anterior
        val last = words.last()
        words[words.lastIndex] = last.copy(text = last.text + clean)
      }
    }*/
    tokens.zip(timestamps).forEach { (token, time) ->
      // Byte 32 = espacio → marca inicio de palabra nueva
      val startsWithSpace = token.startsWith(" ")
      val clean = token.trimStart(' ')

      if (clean.isEmpty()) return@forEach

      // Apóstrofe solo → fusionar con palabra anterior sin espacio
      val isApostrophe = clean == "'"

      val isNewWord = startsWithSpace && !isApostrophe && words.isNotEmpty()
        || words.isEmpty()

      if (isNewWord) {
        words.add(Word(text = clean, startTime = time))
      } else {
        val last = words.last()
        words[words.lastIndex] = last.copy(text = last.text + clean)
      }
    }
    // ── 2. Construir LRC extended ──────────────────────────────────────
    val sb = StringBuilder()
    if (title.isNotEmpty()) sb.append("[ti:$title]\n")
    sb.append("[by:clip-library (sherpa-onnx)]\n\n")

    // Agrupar palabras en líneas de ~8
    groupWordsIntoLines(words).forEach { lineWords ->
      val lineStart = lineWords.first().startTime

      // Timestamp de línea
      sb.append("[${formatLrcTime(lineStart)}]")

      // Inline timestamp por cada palabra ya fusionada
      lineWords.forEach { word ->
        sb.append("<${formatLrcTime(word.startTime)}>${word.text} ")
      }
      sb.append("\n")
    }
    return sb.toString()
  }

  private fun formatLrcTime(seconds: Float): String {
    val totalMs = (seconds * 1000).toLong()
    val min = totalMs / 60000
    val sec = (totalMs % 60000) / 1000
    val cs  = (totalMs % 1000) / 10
    return "%02d:%02d.%02d".format(min, sec, cs)
  }

  private fun groupWordsIntoLines(
    words: List<Word>,
    maxWords: Int = 8,
    maxSilenceMs: Float = 1500f  // pausa > 1.5s = nueva línea
  ): List<List<Word>> {
    val lines = mutableListOf<MutableList<Word>>()
    var current = mutableListOf<Word>()

    words.forEachIndexed { i, word ->
      val prevEnd = if (i == 0) word.startTime
      else words[i - 1].startTime + 0.3f // estimación fin palabra

      val silenceSinceLastWord = (word.startTime - prevEnd) * 1000f
      val lineTooBig = current.size >= maxWords
      val longPause = silenceSinceLastWord > maxSilenceMs

      if ((lineTooBig || longPause) && current.isNotEmpty()) {
        lines.add(current)
        current = mutableListOf()
      }
      current.add(word)
    }
    if (current.isNotEmpty()) lines.add(current)
    return lines
  }
}
