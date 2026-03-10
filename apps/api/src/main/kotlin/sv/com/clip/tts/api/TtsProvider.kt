package sv.com.clip.tts.api

interface TtsProvider {
  fun getAudioUrl(text: String): String
}
