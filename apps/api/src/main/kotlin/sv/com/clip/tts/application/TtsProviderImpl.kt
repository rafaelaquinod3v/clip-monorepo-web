package sv.com.clip.tts.application

import org.springframework.stereotype.Component
import sv.com.clip.tts.api.TtsProvider
import sv.com.clip.tts.domain.TtsPort

@Component
class TtsProviderImpl(
  private val ttsAdapter: TtsPort
) : TtsProvider {
  override fun getAudioUrl(text: String): String {
    //val result = ttsAdapter.streamTextSplitBySentenceSync(text, "af_heart", null)
    // lógica de cache, guardar en MinIO, devolver URL...
    return "https://..."
  }
}
