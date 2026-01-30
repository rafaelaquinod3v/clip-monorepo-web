package sv.com.clip.learning.application.usecases

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sv.com.clip.dictionary.api.DictionaryExternal
import sv.com.clip.learning.domain.UserWord
import sv.com.clip.learning.domain.commands.AddUserWordCommand
import sv.com.clip.learning.domain.repository.UserWordExclusionRepository
import sv.com.clip.learning.domain.repository.UserWordRepository

@Service
class AddUserWordCommandHandler(
  private val userWordRepository: UserWordRepository,
  private val exclusions: UserWordExclusionRepository,
  private val dictionaryExternal: DictionaryExternal,
) {
  @Transactional
  fun handle(command: AddUserWordCommand) {
    val cleanTerm = command.lemma.lowercase().trim()

    // 1. Buscamos el lemma y la entrada oficial simultáneamente
    val official = dictionaryExternal.determineLemmaInfo(cleanTerm)

    val lemma = official?.lemma ?: cleanTerm
    val officialId = official?.id // El UUID del diccionario principal

    exclusions.deleteExclusion(command.userId, lemma)

    val existing = userWordRepository.findByUserIdAndLemma(command.userId, lemma)

    if (existing != null) {
      existing.targetGloss = command.lemma
      existing.status = command.status
      // Actualizamos el ID por si antes era null y ahora existe en el diccionario
      existing.sourceLexicalEntryId = officialId
      userWordRepository.save(existing)
    }else {
      val newWord = UserWord(
        userId = command.userId,
        lemma = lemma,
        sourceLexicalEntryId = officialId,
        targetGloss = command.lemma,
        status = command.status,
        // Es manual solo si NO se encontró en el diccionario oficial
        isManualLexicalEntry = (officialId == null),
      )
      userWordRepository.save(newWord)
    }
  }
}
