package sv.com.clip.dictionary.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sv.com.clip.dictionary.api.AiDataDTO
import sv.com.clip.dictionary.api.DictionaryExternal
import sv.com.clip.dictionary.api.FullWordContextDTO
import sv.com.clip.dictionary.api.LemmaFoundDTO
import sv.com.clip.dictionary.api.LemmaDTO
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
) : DictionaryExternal {

  @Transactional(readOnly = true)
  override fun getFormsLemma(forms: Set<String>): List<LemmaDTO> {

    if (forms.isEmpty()) return emptyList()

    // 1. Get English Lexicon ID (Ideally from a Cache)
    val sourceId = lexiconProvider.findByLang(Language.EN)?.id?.uuid

    return lexicalEntryRepository.findLemmaProjections(forms, sourceId!!)
//    return entities
//      .groupBy { it.term } // Agrupar por el texto del lemma
//      .map { (word, variations) ->
//        val firstEntity = variations.first()
//        TermDTO(
//          id = firstEntity.id,
//          term = word,
//        )
//    }
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
    val sourceId = lexiconProvider.findByLang(Language.EN)?.id?.uuid ?: return null
    val targetId = lexiconProvider.findByLang(Language.ES)?.id?.uuid ?: return null

    val results = lexicalEntryRepository.findFullContext(cleanTerm, sourceId, targetId)
    if (results.isEmpty()) return null

    // 1. Group by Source (English Entry)
    val merged = results.groupBy { it.sourceLexicalEntryId }
      .map { (_, variations) ->
        val first = variations.first()

        // KEY FIX: Extract ALL unique target lemmas/ids found for this English word
        val allTargetLemmas = variations.mapNotNull { it.targetLemma }.distinct()
        val allTargetIds = variations.mapNotNull { it.targetLexicalEntryId }.distinct()

        first.copy(
          // Join all Spanish translations: "miel, cariño"
          targetLemma = allTargetLemmas.joinToString(", "),
          sourceGloss = variations.mapNotNull { it.sourceGloss }.distinct().joinToString("; "),
          targetGloss = variations.mapNotNull { it.targetGloss }.distinct().joinToString("; "),
          // We keep the first ID for forms, or better yet, handle multiple below
          targetLexicalEntryId = allTargetIds.firstOrNull()
        )
      }.firstOrNull()

    // 2. Source Forms (remains the same)
    merged?.sourceLexicalEntryId?.let { sId ->
      val sForms = lexicalEntryRepository.findFormsByLexicalEntryId(sId)
      merged.sourceForms = "${merged.sourceLemma} (${sForms.joinToString(", ")})"
    }

    // 3. ENRICH MULTIPLE TARGET FORMS
    // If you want forms for ALL target lemmas found:
    val allTargetIds = results.mapNotNull { it.targetLexicalEntryId }.distinct()
    if (allTargetIds.isNotEmpty()) {
      val allFormsCombined = allTargetIds.map { tId ->
        val lemma = results.find { it.targetLexicalEntryId == tId }?.targetLemma
        val forms = lexicalEntryRepository.findFormsByLexicalEntryId(tId)
        "$lemma (${forms.joinToString(", ")})"
      }
      merged?.targetForms = allFormsCombined.joinToString(" | ")
    }

    return merged
  }

  override fun generateAiSourceAndTargetLemma(term: String): AiDataDTO? {
    return aiService.getAiDefinition(term)
  }
}
