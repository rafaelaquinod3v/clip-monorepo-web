package sv.com.clip.dictionary.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sv.com.clip.dictionary.api.AiDataDTO
import sv.com.clip.dictionary.api.DictionaryExternal
import sv.com.clip.dictionary.api.FullWordContextDTO
import sv.com.clip.dictionary.api.LemmaFoundDTO
import sv.com.clip.dictionary.api.WordDTO
import sv.com.clip.dictionary.api.WordTranslationDTO
import sv.com.clip.dictionary.domain.queries.LexiconProvider
import sv.com.clip.dictionary.domain.repository.LexicalEntryRepository
import sv.com.clip.dictionary.domain.valueObjects.Language
import sv.com.clip.dictionary.infrastructure.gateways.AiDefinitionService
import sv.com.clip.dictionary.infrastructure.gateways.LemmatizerService

@Service
internal class DictionaryService(
  private val lexicalEntryRepository: LexicalEntryRepository,
  private val lexiconProvider: LexiconProvider,
  private val lemmatizerService: LemmatizerService,
  private val aiService: AiDefinitionService
) : DictionaryExternal{

  @Transactional(readOnly = true)
  override fun getWords(terms: Set<String>): List<WordDTO> {

    val entities = lexicalEntryRepository.findProjectionsByLemmaIn(terms)
  return entities
    .groupBy { it.term } // Agrupar por el texto del lemma
    .map { (word, variations) ->
      val firstEntity = variations.first()
      WordDTO(
        id = firstEntity.id,
        // Unimos todos los sentidos únicos de todas las variaciones encontradas
        definition = variations.mapNotNull { it.definition }
          .distinct()
          .joinToString("; "),
        term = word,
      )
    }
}

  override fun lemmatize(term: String): String {
    return lemmatizerService.lemmatize(term)
  }

  override fun getFullDefinition(
    term: String,
  ): List<WordTranslationDTO> {
    val lexiconEn = lexiconProvider.findByLang(Language.EN)?.id?.uuid
    val lexiconEs = lexiconProvider.findByLang(Language.ES)?.id?.uuid
    return lexicalEntryRepository.findFullDefinition(term, lexiconEn!!, lexiconEs!!)
  }

  @Transactional(readOnly = true)
  override fun determineLemma(term: String): String? {
    return lexicalEntryRepository.findLemmasByForm(term).firstOrNull()
  }

  @Transactional(readOnly = true)
  override fun determineLemmaInfo(term: String): LemmaFoundDTO? {
    return lexicalEntryRepository.findLemmasByForms(term).firstOrNull()
  }

  @Transactional(readOnly = true)
  override fun findFullDefinition(term: String): FullWordContextDTO? {
    val cleanTerm = term.lowercase().trim()

    // 1. Resolve Lexicon IDs (Ideally cached)
    val sourceId = lexiconProvider.findByLang(Language.EN)?.id?.uuid
    val targetId = lexiconProvider.findByLang(Language.ES)?.id?.uuid

    // 2. Query the DB
    val results = lexicalEntryRepository.findFullContext(cleanTerm, sourceId!!, targetId!!)
    if(results.isEmpty()) return null

    // 3. Logic: If multiple senses exist, we pick the first one or
    // group them if you want a joined string.
    val merged = results.groupBy { it.sourceLexicalEntryId }
      .map { (_, variations) ->
        val first = variations.first()
        first.copy(
          sourceGloss = variations.mapNotNull { it.sourceGloss }.distinct().joinToString("; "),
          targetGloss = variations.mapNotNull { it.targetGloss }.distinct().joinToString("; ")
        )
      }.firstOrNull()

    // 3. Enrich with Source Forms (e.g., "bank (banks, banking)")
    val sForms = lexicalEntryRepository.findFormsByLexicalEntryId(merged?.sourceLexicalEntryId!!)
    merged.sourceForms = "${merged.sourceLemma} (${sForms.joinToString(", ")})"

    // 2. Fetch Target Forms only if target exists
    merged.targetLexicalEntryId?.let { targetUuid ->
      val targetForms = lexicalEntryRepository.findFormsByLexicalEntryId(targetUuid)
      // Combine Target Lemma + its Forms: "Perro (perros, perra)"
      merged.targetForms = "${merged.targetLemma} (${targetForms.joinToString(", ")})"
    }

    return merged
  }

  override fun generateDefinition(term: String): AiDataDTO? {
    return aiService.getAiDefinition(term)
  }
}
