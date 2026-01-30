package sv.com.clip.dictionary.api

interface DictionaryExternal {
  fun getFormsLemma(forms: Set<String>): List<LemmaDTO>
  fun lemmatize(term: String): String
  fun getFullDefinition(term: String): List<WordTranslationDTO>
  fun determineLemma(term: String): String?
  fun determineLemmaInfo(term: String): LemmaFoundDTO?
  fun findFullDefinition(term: String): FullWordContextDTO?
  fun generateAiSourceAndTargetLemma(term: String): AiDataDTO?
}
