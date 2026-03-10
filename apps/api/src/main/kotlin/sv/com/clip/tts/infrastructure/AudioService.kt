package sv.com.clip.tts.infrastructure

import org.springframework.stereotype.Service
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.encode.AudioAttributes
import ws.schild.jave.encode.EncodingAttributes
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Service
class AudioService {

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
}
