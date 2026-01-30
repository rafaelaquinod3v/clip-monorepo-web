package sv.com.clip.dictionary.api

interface DictionaryExternal {
  fun getWords(terms: Set<String>): List<WordDTO>
  fun lemmatize(term: String): String
  fun getFullDefinition(term: String): List<WordTranslationDTO>
  fun determineLemma(term: String): String?
  fun determineLemmaInfo(term: String): LemmaFoundDTO?
  fun findFullDefinition(term: String): FullWordContextDTO?
  fun generateDefinition(term: String): AiDataDTO?
}
