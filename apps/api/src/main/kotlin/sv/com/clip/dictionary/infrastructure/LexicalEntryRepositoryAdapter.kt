package sv.com.clip.dictionary.infrastructure

import org.springframework.stereotype.Repository
import sv.com.clip.dictionary.api.FullWordContextDTO
import sv.com.clip.dictionary.api.LemmaFoundDTO
import sv.com.clip.dictionary.api.WordDTO
import sv.com.clip.dictionary.api.WordTranslationDTO
import sv.com.clip.dictionary.domain.model.LexicalEntry
import sv.com.clip.dictionary.domain.repository.LexicalEntryRepository
import sv.com.clip.dictionary.infrastructure.persistence.jpa.LexicalEntryEntity
import sv.com.clip.dictionary.infrastructure.persistence.repository.JpaLexicalEntryEntityRepository
import java.util.UUID

@Repository
class LexicalEntryRepositoryAdapter(
  private val jpaRepository: JpaLexicalEntryEntityRepository
) : LexicalEntryRepository {

  override fun save(lexicalEntry: LexicalEntry): LexicalEntry {
    return jpaRepository.save(LexicalEntryEntity.fromDomain(lexicalEntry)).toDomain()
  }

  override fun findAll(): List<LexicalEntry> {
    return jpaRepository.findAll().map { it.toDomain() }
  }

  override fun findAllByLemmaIn(lemmas: Set<String>): List<LexicalEntry> {
    return jpaRepository.findAllByLemmaInWithSenses(lemmas).map { it.toDomain() }
  }

  override fun findAllByLemmaInWithDetails(lemmas: Set<String>): List<LexicalEntry> {
    return jpaRepository.findAllByLemmaInWithDetails(lemmas).map { it.toDomain() }
  }

  override fun findProjectionsByLemmaIn(lemmas: Set<String>): List<WordDTO> {
    return jpaRepository.findProjectionsByLemmaIn(lemmas)
  }

  override fun findFullDefinition(
    term: String,
    sourceLexiconId: UUID,
    targetLexiconId: UUID
  ): List<WordTranslationDTO> {
    return jpaRepository.findFullDefinition(term, sourceLexiconId, targetLexiconId)
  }

  override fun saveAll(list: List<LexicalEntry>): List<LexicalEntry> {
    return jpaRepository.saveAll(list.map { LexicalEntryEntity.fromDomain(it) }).map { it.toDomain() }
  }

  override fun findLemmasByForm(term: String): List<String> {
    return jpaRepository.findLemmasByForm(term)
  }

  override fun findLemmasByForms(term: String): List<LemmaFoundDTO> {
    return jpaRepository.findLemmasByForms(term)
  }

  override fun findFullContext(
    term: String,
    sourceLexiconId: UUID,
    targetLexiconId: UUID
  ): List<FullWordContextDTO> {
    return jpaRepository.findFullContext(term, sourceLexiconId, targetLexiconId)
  }

  override fun findFormsByLexicalEntryId(id: UUID): List<String> {
    return jpaRepository.findFormsByLexicalEntryId(id)
  }
}
