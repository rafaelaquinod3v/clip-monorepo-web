package sv.com.clip.speech.api

interface SpeechAudioExternal {
  fun getSpeechAudio(rawText: String): ByteArray
}
