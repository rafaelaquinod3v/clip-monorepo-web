package sv.com.clip.learning.infrastructure.repository

import org.springframework.stereotype.Repository
import sv.com.clip.learning.domain.UserWord
import sv.com.clip.learning.domain.WordStatus
import sv.com.clip.learning.domain.repository.UserWordRepository
import sv.com.clip.learning.infrastructure.jpa.UserWordEntity
import java.util.UUID

@Repository
class UserWordRepositoryAdapter(
  private val jpaUserWordRepository : JpaUserWordRepository
) : UserWordRepository {
  override fun findByUserIdAndLemma(userId: UUID, lemma: String): UserWord? {
    return jpaUserWordRepository.findByUserIdAndLemma(userId, lemma)?.toDomain()
  }

  override fun findAllByLemmaIn(lemmas: Set<String>): List<UserWord> {
    return jpaUserWordRepository.findAllByLemmaIn(lemmas).map { it.toDomain() }
  }

  override fun save(userWord: UserWord): UserWord {
    return jpaUserWordRepository.save(UserWordEntity.fromDomain(userWord)).toDomain()
  }

  override fun findAllByUserId(userId: UUID): List<UserWord> {
    return jpaUserWordRepository.findAllByUserId(userId).map { it.toDomain() }
  }

  override fun deleteByUserIdAndLemma(userId: UUID, lemma: String) {
    jpaUserWordRepository.deleteByUserIdAndLemma(userId, lemma)
  }

  override fun updateUserWordStatus(
    userId: UUID,
    lemma: String,
    status: WordStatus
  ) {
    jpaUserWordRepository.updateUserWordStatus(userId, lemma, status)
  }

  override fun findById(id: UUID): UserWord? {
    return jpaUserWordRepository.findById(id)?.get()?.toDomain()
  }
}
