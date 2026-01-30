package sv.com.clip.learning.infrastructure.repository

import org.springframework.data.jpa.repository.JpaRepository
import sv.com.clip.learning.infrastructure.jpa.UserWordExclusionEntity
import java.util.UUID

interface JpaUserWordExclusionRepository : JpaRepository<UserWordExclusionEntity, UUID> {
  fun findAllByLemmaIn(lemmas: Set<String>) : Set<UserWordExclusionEntity>
  fun deleteByLemma(lemma: String)
}
