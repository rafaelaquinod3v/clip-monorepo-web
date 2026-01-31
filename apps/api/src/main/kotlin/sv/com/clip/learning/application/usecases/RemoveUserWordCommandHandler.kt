package sv.com.clip.learning.application.usecases

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sv.com.clip.dictionary.api.DictionaryExternal
import sv.com.clip.learning.domain.commands.RemoveUserWordCommand
import sv.com.clip.learning.domain.repository.UserWordRepository

@Service
class RemoveUserWordCommandHandler(
  private val userWordRepository: UserWordRepository,
  private val dictionaryExternal: DictionaryExternal,
) {

  @Transactional
  fun handle(command: RemoveUserWordCommand) {
    val cleanTerm = command.term.lowercase().trim()
    val lemma = dictionaryExternal.determineLemma(cleanTerm) ?: cleanTerm
    userWordRepository.deleteByUserIdAndLemma(command.userId, lemma)
  }
}
