package sv.com.clip.dictionary.infrastructure.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import sv.com.clip.dictionary.api.LemmaFoundDTO
import sv.com.clip.dictionary.api.WordDTO
import sv.com.clip.dictionary.api.WordTranslationDTO
import sv.com.clip.dictionary.infrastructure.persistence.jpa.LexicalEntryEntity
import java.util.UUID

interface JpaLexicalEntryEntityRepository : JpaRepository<LexicalEntryEntity, UUID> {
  fun findAllByLemmaIn(lemmas: Set<String>) : List<LexicalEntryEntity>

  @Query("SELECT DISTINCT l FROM LexicalEntryEntity l LEFT JOIN FETCH l._senses WHERE l.lemma IN :lemmas")
  fun findAllByLemmaInWithSenses(@Param("lemmas") lemmas: Set<String>): Set<LexicalEntryEntity>

  @Query("""
        SELECT DISTINCT l FROM LexicalEntryEntity l
        LEFT JOIN FETCH l._senses
        LEFT JOIN FETCH l._forms
        WHERE l.lemma IN :lemmas
  """)
  fun findAllByLemmaInWithDetails(@Param("lemmas") lemmas: Set<String>): Set<LexicalEntryEntity>


  @Query("""
        SELECT new sv.com.clip.dictionary.api.WordDTO(l.id, l.lemma, s.definition)
        FROM LexicalEntryEntity l
        JOIN l._senses s
        WHERE l.lemma IN :lemmas
    """)
  fun findProjectionsByLemmaIn(@Param("lemmas") lemmas: Set<String>): List<WordDTO>

  @Query("""
    SELECT DISTINCT le FROM LexicalEntryEntity le
    LEFT JOIN le._forms f
    WHERE le.lemma = :lemma
       OR f.writtenRepresentation = :word
""")
  fun findByWordInAnyForm(@Param("lemma") lemma: String): List<LexicalEntryEntity>

  @Query("""
    SELECT DISTINCT new sv.com.clip.dictionary.api.WordTranslationDTO(
        le.id,
        le.lemma,
        target_le.lemma
    )
    FROM LexicalEntryEntity le
    JOIN le._senses s
    INNER JOIN LexicalEntryEntity target_le ON target_le.lexiconId = :targetLexiconId
    INNER JOIN target_le._senses target_s ON target_s.conceptIli = s.conceptIli
    WHERE (le.lemma = :term OR EXISTS (SELECT f FROM le._forms f WHERE f.writtenRepresentation = :term))
      AND le.lexiconId = :sourceLexiconId
""")
  fun findFullDefinition(
    @Param("term") term: String,
    @Param("sourceLexiconId") sourceId: UUID,
    @Param("targetLexiconId") targetId: UUID
  ): List<WordTranslationDTO>


  @Query("""
    SELECT DISTINCT new sv.com.clip.dictionary.api.WordTranslationDTO(
        source_le.id,
        source_le.lemma,
        target_le.lemma
    )
    FROM LexicalEntryEntity source_le
    JOIN source_le._senses source_s
    JOIN LexicalEntryEntity target_le ON target_le.lexiconId = :targetLexiconId
    JOIN target_le._senses target_s ON target_s.conceptIli = source_s.conceptIli
    WHERE (source_le.lemma = :lemma OR EXISTS (SELECT f FROM source_le._forms f WHERE f.writtenRepresentation = :term))
    AND source_le.lexiconId = :sourceLexiconId
""")
  fun findTranslationBetweenLanguages(
    @Param("term") term: String,
    @Param("lemma") lemma: String,
    @Param("sourceLexiconId") sourceId: UUID,
    @Param("targetLexiconId") targetId: UUID
  ): List<WordTranslationDTO>

  @Query(value = """
      SELECT lemma FROM lexical_entries WHERE lemma = :term
      UNION
      SELECT le.lemma FROM lexical_entries le
      JOIN lexical_entry_forms f ON le.id = f.lexical_entry_id
      WHERE f.written_representation = :term
  """, nativeQuery = true)
  fun findLemmasByForm(@Param("term") term: String): List<String>

  @Query("""
    SELECT DISTINCT new sv.com.clip.dictionary.api.LemmaFoundDTO(le.id, le.lemma)
    FROM LexicalEntryEntity le
    LEFT JOIN le._forms f
    WHERE le.lemma = :term OR f.writtenRepresentation = :term
    """)
  fun findLemmasByForms(@Param("term") term: String): List<LemmaFoundDTO>

}
