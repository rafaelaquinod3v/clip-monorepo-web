package sv.com.clip.dictionary.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sv.com.clip.dictionary.api.DictionaryExternal
import sv.com.clip.dictionary.api.LemmaFoundDTO
import sv.com.clip.dictionary.api.WordDTO
import sv.com.clip.dictionary.api.WordTranslationDTO
import sv.com.clip.dictionary.domain.queries.LexiconProvider
import sv.com.clip.dictionary.domain.repository.LexicalEntryRepository
import sv.com.clip.dictionary.domain.valueObjects.Language
import sv.com.clip.dictionary.infrastructure.gateways.LemmatizerService

@Service
internal class DictionaryService(
  private val lexicalEntryRepository: LexicalEntryRepository,
  private val lexiconProvider: LexiconProvider,
  private val lemmatizerService: LemmatizerService,
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
}
