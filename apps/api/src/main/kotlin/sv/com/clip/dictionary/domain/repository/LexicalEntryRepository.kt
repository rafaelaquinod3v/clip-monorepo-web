package sv.com.clip.dictionary.domain.repository

import org.jmolecules.ddd.annotation.Repository
import sv.com.clip.dictionary.api.FullWordContextDTO
import sv.com.clip.dictionary.api.LemmaFoundDTO
import sv.com.clip.dictionary.api.WordDTO
import sv.com.clip.dictionary.api.WordTranslationDTO
import sv.com.clip.dictionary.domain.model.LexicalEntry
import java.util.UUID

@Repository
interface LexicalEntryRepository {
  fun save(lexicalEntry: LexicalEntry) : LexicalEntry
  fun findAll() : List<LexicalEntry> // TODO: pagination,
  fun findAllByLemmaIn(lemmas: Set<String>) : List<LexicalEntry>
  fun findAllByLemmaInWithDetails(lemmas: Set<String>) : List<LexicalEntry>
  fun findProjectionsByLemmaIn(lemmas: Set<String>) : List<WordDTO>
  fun findFullDefinition(term: String, sourceLexiconId: UUID, targetLexiconId: UUID): List<WordTranslationDTO>
  fun saveAll(list: List<LexicalEntry>) : List<LexicalEntry>
  fun findLemmasByForm(term: String): List<String>
  fun findLemmasByForms(term: String): List<LemmaFoundDTO>
  fun findFullContext(term: String, sourceLexiconId: UUID, targetLexiconId: UUID): List<FullWordContextDTO>
  fun findFormsByLexicalEntryId(id: UUID) : List<String>
}
