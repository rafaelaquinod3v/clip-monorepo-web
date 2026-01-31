package sv.com.clip.learning.application.usecases

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sv.com.clip.dictionary.api.DictionaryExternal
import sv.com.clip.learning.domain.commands.UpdateUserWordStatusCommand
import sv.com.clip.learning.domain.repository.UserWordRepository

@Service
class UpdateUserWordStatusCommandHandler(
  private val userWordRepository: UserWordRepository,
  private val dictionaryExternal: DictionaryExternal,
) {

  @Transactional
  fun handle(command: UpdateUserWordStatusCommand) {
    val cleanTerm = command.term.lowercase().trim()
    val lemma = dictionaryExternal.determineLemma(cleanTerm) ?: cleanTerm
    userWordRepository.updateUserWordStatus(command.userId, lemma, command.status)
  }
}
