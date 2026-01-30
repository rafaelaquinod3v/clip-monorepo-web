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
//    val official = dictionaryExternal.determineLemmaInfo(cleanTerm)
    // 1. Fetch FULL context (Lemma + Translations + ILI) in one fast call
    // Your DictionaryExternal should now return a DTO with targetLemma, targetId, etc.
    val officialData = dictionaryExternal.findFullDefinition(cleanTerm)

    val lemma = officialData?.sourceLemma ?: cleanTerm
    val sourceId = officialData?.sourceLexicalEntryId // El UUID del diccionario principal
    val targetId = officialData?.targetLexicalEntryId

    // Logic: If Dict has English but no Spanish translation, use AI Fallback
    val finalTargetLemma = when {
      officialData?.targetLemma != null -> officialData.targetLemma
//      sourceId != null -> aiService.getQuickTranslation(lemma) // Gemma 2:2b
      else -> command.lemma // User's manual entry
    }

    exclusions.deleteExclusion(command.userId, lemma)

    val existing = userWordRepository.findByUserIdAndLemma(command.userId, lemma)

    if (existing != null) {
      existing.status = command.status
      existing.targetLemma = finalTargetLemma

      existing.targetGloss = officialData?.targetGloss
      existing.sourceGloss = officialData?.sourceGloss

      existing.sourceForms = officialData?.sourceForms
      existing.targetForms = officialData?.targetForms

      // Actualizamos el ID por si antes era null y ahora existe en el diccionario
      existing.sourceLexicalEntryId = sourceId
      existing.targetLexicalEntryId = targetId
      userWordRepository.save(existing)
    }else {
      val newWord = UserWord(
        userId = command.userId,
        lemma = lemma,
        status = command.status,
        targetLemma = finalTargetLemma,
        sourceLexicalEntryId = sourceId,
        targetLexicalEntryId = targetId,
        sourceGloss = officialData?.sourceGloss,
        targetGloss = officialData?.targetGloss,
        sourceForms = officialData?.sourceForms,
        targetForms = officialData?.targetForms,
        // Es manual solo si NO se encontró en el diccionario oficial
        isManualLexicalEntry = (sourceId == null),
      )
      userWordRepository.save(newWord)
    }
  }
}
