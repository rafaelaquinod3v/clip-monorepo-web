package sv.com.clip.learning.application.usecases

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sv.com.clip.dictionary.api.DictionaryExternal
import sv.com.clip.learning.domain.commands.RemoveUserWordExclusionCommand
import sv.com.clip.learning.infrastructure.UserWordExclusionAdapter

@Service
class RemoveUserWordExclusionCommandHandler(
  private val exclusionAdapter: UserWordExclusionAdapter,
  private val external: DictionaryExternal,
) {
  @Transactional
  fun handle(command: RemoveUserWordExclusionCommand) {
    val cleanTerm = command.term.lowercase().trim()

    val lemma = external.determineLemma(cleanTerm) ?: cleanTerm

    exclusionAdapter.deleteExclusion(command.userId, lemma)
  }
}
