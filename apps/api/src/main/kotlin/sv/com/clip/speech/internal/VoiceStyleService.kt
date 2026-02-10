package sv.com.clip.speech.internal

import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Service
class VoiceStyleService {

  fun loadStyle(voiceName: String = "af_heart"): FloatArray {
    // Carga el archivo desde resources/voices/af_heart.bin
    val inputStream: InputStream = this::class.java.classLoader
      .getResourceAsStream("models/kokoro-82M-v1_0-ONNX/$voiceName.bin")
      ?: throw RuntimeException("Voz no encontrada")

    val bytes = inputStream.readAllBytes()
    val floatBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
    // Create an array for JUST one style (256 floats)
    val singleStyle = FloatArray(256)

    // Read only the first 256 elements from the 130,560 available
    floatBuffer.get(singleStyle)

    return singleStyle
/*    val styleArray = FloatArray(floatBuffer.remaining())
    floatBuffer.get(styleArray)
    println(styleArray.size)
    return styleArray*/ // Esto debería tener tamaño 256
  }
}
