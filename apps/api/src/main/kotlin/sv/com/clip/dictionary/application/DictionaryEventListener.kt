package sv.com.clip.dictionary.application

import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import sv.com.clip.dictionary.domain.model.LexicalEntry
import sv.com.clip.dictionary.domain.model.LexicalEntryId
import sv.com.clip.dictionary.domain.queries.LexiconProvider
import sv.com.clip.dictionary.domain.repository.LexicalEntryRepository
import sv.com.clip.dictionary.domain.valueObjects.Language
import sv.com.clip.dictionary.domain.valueObjects.PartOfSpeech
import sv.com.clip.learning.domain.events.TermsNotFoundEvent

@Component
class DictionaryEventListener(
  private val repository: LexicalEntryRepository,
  private val lexiconProvider: LexiconProvider,
) {
  // @ApplicationModuleListener asegura que el evento se maneje
  // de forma transaccional y asíncrona (específico de Spring Modulith)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @ApplicationModuleListener
  fun onWordsNotFound(event: TermsNotFoundEvent) {
    println("EVENTO RECIBIDO!") // Debería imprimirse inmediatamente
    println("Telemetria") // TODO:
    val lexicon = lexiconProvider.findByLang(Language.EN) ?: return
    val newEntries = event.terms.map { term ->
        LexicalEntry(
          id = LexicalEntryId.generate(),
          lemma = term,
          sourceId = "USER_DISCOVERED",
          lexiconId = lexicon.id,
          partOfSpeech = PartOfSpeech.NOUN
        )
      }
//      }.apply {
//        addSense(SenseEntity(definition = "Pending review", lexicalEntryEntity = this))
//      }
    // 3. Persistimos la lista limpia
//    if (newEntries.isNotEmpty()) {
//      repository.saveAll(newEntries)
//    }
  }
}
