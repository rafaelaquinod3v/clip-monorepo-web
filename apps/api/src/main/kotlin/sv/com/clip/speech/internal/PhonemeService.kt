package sv.com.clip.speech.internal

import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import org.springframework.stereotype.Service
import java.lang.ref.Reference

@Service
class PhonemeService {
  companion object {
    private val ESPEAK_LOCK = Any()
    private var isInitialized = false
  }

  fun getPhonemes(text: String): String {
    if (text.isBlank()) return ""

    val bytes = text.toByteArray(Charsets.UTF_8)
    val textMem = Memory((bytes.size + 1).toLong())
    textMem.write(0, bytes, 0, bytes.size)
    textMem.setByte(bytes.size.toLong(), 0.toByte())

    val ptrRef = PointerByReference(textMem)
    val sb = StringBuilder()

    synchronized(ESPEAK_LOCK) {
      try {
        // Ensure initialization is fresh for this context
        if (!isInitialized) {
          EspeakNG.INSTANCE.espeak_Initialize(EspeakNG.AUDIO_OUTPUT_RETRIVAL, 0, null, 0)
          EspeakNG.INSTANCE.espeak_SetVoiceByName("en")
          isInitialized = true
        }

        while (true) {
          // CRITICAL: use .value, NOT .pointer
          val currentPos = ptrRef.value
          if (currentPos == null || currentPos.getByte(0) == 0.toByte()) break

          val resPtr = EspeakNG.INSTANCE.espeak_TextToPhonemes(ptrRef, EspeakNG.espeakCHARS_AUTO, EspeakNG.espeakPHONEMES_IPA)

          if (resPtr != null) {
            sb.append(resPtr.getString(0, "UTF-8")).append(" ")
          } else {
            // Manual skip to prevent infinite loop/crash
            val next = Pointer(Pointer.nativeValue(ptrRef.value) + 1)
            ptrRef.value = next
          }
        }
      } catch (t: Throwable) {
        println("Native call failed: ${t.message}")
      } finally {
        Reference.reachabilityFence(textMem)
        Reference.reachabilityFence(ptrRef)
      }
    }
    return sb.toString().trim()
  }
}

