package sv.com.clip.text.api

interface TextProcessorExternal {
  fun splitBySentence(text: String): List<String>
}
