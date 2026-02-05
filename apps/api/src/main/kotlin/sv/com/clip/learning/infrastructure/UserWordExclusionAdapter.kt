package sv.com.clip.learning.infrastructure

import org.springframework.stereotype.Component
import sv.com.clip.learning.domain.repository.UserWordExclusionRepository
import sv.com.clip.learning.infrastructure.jpa.UserWordExclusionEntity
import sv.com.clip.learning.infrastructure.repository.JpaUserWordExclusionRepository
import java.util.UUID

@Component
class UserWordExclusionAdapter(
  private val exclusionRepository: JpaUserWordExclusionRepository,
) : UserWordExclusionRepository {
  override fun findExclusions(
    userId: UUID, //TODO: use this
  ): Set<String> {
    return exclusionRepository.findAllByUserId(userId).map { it.lemma }.toSet()
  }

  override fun saveExclusion(userId: UUID, lemma: String) {
    exclusionRepository.save(UserWordExclusionEntity(UUID.randomUUID(), userId, lemma))
  }

  override fun deleteExclusion(userId: UUID, lemma: String) {
    exclusionRepository.deleteByUserIdAndLemma(userId, lemma)
  }

  override fun isExcluded(userId: UUID, lemma: String): Boolean {
    // return repo.existsByUserIdAndLemma(userId, word)
    TODO("Not yet implemented")
  }

}
