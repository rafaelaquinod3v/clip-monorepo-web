package sv.com.clip.tts.domain

import java.io.OutputStream

fun interface TtsPort {
  fun textSplitBySentenceSync(fullText: String, voice: String, outputStream: OutputStream)
}
