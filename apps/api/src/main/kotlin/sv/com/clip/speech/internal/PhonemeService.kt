package sv.com.clip.speech.internal

import com.sun.jna.Memory
import com.sun.jna.Pointer
import org.springframework.stereotype.Service
import java.lang.ref.Reference

@Service
class PhonemeService { // Tokenizador (G2P)
  private val lock = Any()
/*  init {
    // Inicialización obligatoria de espeak-ng
    // 0 = AUDIO_OUTPUT_PLAYBACK (aunque no lo usemos para sonar, es necesario inicializar)
    val result = EspeakNG.INSTANCE.espeak_Initialize(0, 0, null, 0)
    if (result < 0) throw RuntimeException("No se pudo inicializar espeak-ng")
    println("EspeakNG")
    EspeakNG.INSTANCE.espeak_SetVoiceByName("en-us")
  }*/
/*  init {
    // Intentar con null (ahora que ya creamos la carpeta en /usr/share)
    val result = EspeakNG.INSTANCE.espeak_Initialize(0, 0, null, 0)

    if (result < 0) {
      throw RuntimeException("Fallo crítico: espeak no se pudo inicializar. Código: $result")
    }

    // Intentar cargar la voz. Si esto devuelve != 0, es que no encontró los datos.
    val voiceStatus = EspeakNG.INSTANCE.espeak_SetVoiceByName("en")
    println(">>> Espeak Init: $result | Voice Status: $voiceStatus")
  println(getPhonemes("test"))
  }*/
init {
  synchronized(lock) {
    val result = EspeakNG.INSTANCE.espeak_Initialize(0, 0, null, 0)
    EspeakNG.INSTANCE.espeak_SetVoiceByName("en")
    println(">>> Espeak Init: $result | Voice Status: 0")
    //println(getPhonemes("test"))
  }
}

  fun getPhonemes(text: String): String {
    if (text.isBlank()) return ""

    return synchronized(lock) {
      try {
        // Pasamos el string directo.
        // encoding 1 = UTF8, phonememode 0x01 = IPA
        val ptr = EspeakNG.INSTANCE.espeak_TextToPhonemes(text, 1, 0x01)

        if (ptr == null || Pointer.nativeValue(ptr) == 0L) {
          ""
        } else {
          ptr.getString(0, "UTF-8")
        }
      } catch (e: Exception) {
        ""
      }
    }
  }

  /*  fun getPhonemes(text: String): String {
      if (text.isBlank()) return ""

      println("Tratando de procesar: $text") // Para ver dónde muere

      val bytes = text.toByteArray(Charsets.UTF_8)
      val memory = Memory((bytes.size + 1).toLong())
      memory.write(0, bytes, 0, bytes.size)
      memory.setByte(bytes.size.toLong(), 0.toByte())

      return try {
        // Usamos 0x01 para IPA
        val ptr = EspeakNG.INSTANCE.espeak_TextToPhonemes(memory, 1, 0x01)

        if (ptr == null || Pointer.nativeValue(ptr) == 0L) {
          println("Espeak devolvió puntero nulo")
          ""
        } else {
          val fono = ptr.getString(0, "UTF-8")
          println("Fonemas OK: $fono")
          fono
        }
      } catch (e: Throwable) {
        println("Error nativo atrapado: ${e.message}")
        ""
      } finally {
        Reference.reachabilityFence(memory)
      }
    }*/

/*  fun getPhonemes(text: String): String {
    if (text.isBlank()) return ""
    // El texto debe ser un puntero de C (Memory)
    val input = text
    val bytes = input.toByteArray(Charsets.UTF_8)
    // Reserva tamaño de los bytes + 1 para el terminador nulo
    val memory = Memory((bytes.size + 1).toLong())
    memory.write(0, bytes, 0, bytes.size)
    memory.setByte(bytes.size.toLong(), 0.toByte())
    //memory.setString(0, input)
    //val textPointer = com.sun.jna.Memory((text.length + 1).toLong())
    //textPointer.setString(0, text)

    // 0x01 = EspeakPHONEMES_IPA (para obtener International Phonetic Alphabet)

   // Reference.reachabilityFence(memory)
*//*    if (phonemes == null || Pointer.nativeValue(phonemes) == 0L) {
      throw RuntimeException("La librería nativa devolvió un puntero nulo")
    }
    val result = ptr.getString(0, "UTF-8")*//*
    //return phonemes ?: ""
    try {
      val ptr = EspeakNG.INSTANCE.espeak_TextToPhonemes(memory, 1, 0x01)
      if (ptr == null || Pointer.nativeValue(ptr) == 0L) {
        return ""
      }

      // 3. Leer el String desde el puntero de forma segura
      val result = ptr.getString(0, "UTF-8")
      println("Fonemas generados: $result")
      return result ?: ""

    } catch (e: Exception) {
      println("Error en espeak: ${e.message}")
      return ""
    } finally {
      // 4. Protección contra el Garbage Collector
      Reference.reachabilityFence(memory)
    }
  }*/
}
