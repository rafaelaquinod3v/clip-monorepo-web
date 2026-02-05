package sv.com.clip.learning.infrastructure.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import sv.com.clip.learning.domain.WordStatus
import sv.com.clip.learning.infrastructure.jpa.UserWordEntity
import java.util.*

interface JpaUserWordRepository : JpaRepository<UserWordEntity, UUID> {
  fun findAllByLemmaIn(lemmas: Set<String>) : List<UserWordEntity>
  fun findAllByUserId(userId: UUID): List<UserWordEntity>
  fun deleteByUserIdAndLemma(userId: UUID, lemma: String)
  //fun deleteByLemma(lemma: String)
  fun findByUserIdAndLemma(userId: UUID, lemma: String): UserWordEntity?
  //fun findByLemma(lemma: String): UserWordEntity?
  @Modifying
  @Query("update UserWordEntity set status = :status where lemma = :lemma and userId = :userId")
  fun updateUserWordStatus(@Param("userId") userId: UUID, @Param("lemma") lemma: String, @Param("status") status: WordStatus)
}
