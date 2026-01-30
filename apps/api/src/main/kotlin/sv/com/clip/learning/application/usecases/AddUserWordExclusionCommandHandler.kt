package sv.com.clip.learning.application.usecases

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sv.com.clip.dictionary.api.DictionaryExternal
import sv.com.clip.learning.domain.commands.AddUserWordExclusionCommand
import sv.com.clip.learning.domain.repository.UserWordRepository
import sv.com.clip.learning.infrastructure.UserWordExclusionAdapter

@Service
class AddUserWordExclusionCommandHandler(
  private val exclusionAdapter: UserWordExclusionAdapter,
  private val userWordRepository: UserWordRepository,
  private val external: DictionaryExternal,
) {

  @Transactional
  fun handle(command: AddUserWordExclusionCommand) {
    val cleanTerm: String = command.lemma.lowercase().trim()

    val lemma = external.determineLemma(cleanTerm) ?: cleanTerm

    exclusionAdapter.saveExclusion(command.userId, lemma)

    userWordRepository.deleteByUserIdAndLemma(command.userId, lemma)
  }
}
