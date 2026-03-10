package sv.com.clip.tts.domain

import java.io.OutputStream

fun interface TtsPort {
  fun streamTextSplitBySentenceSync(fullText: String, voice: String, outputStream: OutputStream)
}
