package sv.com.clip.speech.internal

import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
//import org.graalvm.polyglot.Context
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.lang.ref.Reference

@Service
class PhonemeService {
/*  private val context = Context.newBuilder("python")
    .allowAllAccess(true) // Permite que Python acceda a archivos y red si es necesario
    .build()*/
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
    return sanitizePhonemes(sb.toString().trim())
  }

  fun sanitizePhonemes(ipa: String): String {
    return ipa
      .replace("'", "ˈ") // espeak stress to Kokoro stress
      .replace("ˌ", "ˌ") // Secondary stress
      .replace("'", "ˈ")  // Replace standard apostrophe with IPA primary stress
      .replace("g", "ɡ") // Standard g to double-storey ɡ (ID 92)
      .replace("r", "ɹ") // Standard r to alveolar ɹ (ID 123)
    // Add more replacements if you see "MISSING CHAR" logs
  }

/*  fun textToPhonemes(text: String): String {
    // Ejecutamos código Python directamente
    val pyCode = """
            from phonemizer import phonemize
            def get_ipa(text):
                return phonemize(text, language='en-us', backend='espeak')
        """.trimIndent()

    context.eval("python", pyCode)
    val pythonFunction = context.getBindings("python").getMember("get_ipa")

    // Llamamos a la función de Python pasándole el texto de Kotlin
    return pythonFunction.execute(text).asString()
  }*/
private val restClient = RestClient.create("http://localhost:8765")
  fun getPhonemesV2(input: String): PhonemeResponse? {
    return restClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/phonemes")
          .queryParam("text", input) // Agrega ?text=valor
          .build()
      }
      .retrieve()
      .body<PhonemeResponse>()
  }
}

