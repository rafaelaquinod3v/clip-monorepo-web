package sv.com.clip.learning.domain.repository

import org.jmolecules.ddd.annotation.Repository
import sv.com.clip.learning.domain.UserWord
import java.util.UUID

@Repository
interface UserWordRepository {
  fun findByUserIdAndLemma(userId: UUID, lemma: String): UserWord?
  fun findAllByLemmaIn(lemmas: Set<String>) : List<UserWord>
  fun save(userWord: UserWord): UserWord
  fun findAllByUserId(userId: UUID): List<UserWord>
  fun deleteByUserIdAndLemma(userId: UUID, lemma: String)
  fun findById(id: UUID): UserWord?
}
