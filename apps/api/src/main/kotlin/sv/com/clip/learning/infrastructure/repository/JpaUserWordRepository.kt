package sv.com.clip.learning.infrastructure.repository

import org.springframework.data.jpa.repository.JpaRepository
import sv.com.clip.learning.infrastructure.jpa.UserWordEntity
import java.util.UUID

interface JpaUserWordRepository : JpaRepository<UserWordEntity, UUID> {
  fun findAllByLemmaIn(lemmas: Set<String>) : List<UserWordEntity>
  fun deleteByUserIdAndLemma(userId: UUID, lemma: String)
  fun findByUserIdAndLemma(userId: UUID, lemma: String)
  fun findByLemma(lemma: String): UserWordEntity?
}
