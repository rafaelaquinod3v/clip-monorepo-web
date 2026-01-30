package sv.com.clip.learning.application.usecases

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sv.com.clip.dictionary.api.DictionaryExternal
import sv.com.clip.learning.domain.UserWord
import sv.com.clip.learning.domain.commands.AddUserWordCommand
import sv.com.clip.learning.domain.events.EnrichUserWordWithAiEvent
import sv.com.clip.learning.domain.repository.UserWordExclusionRepository
import sv.com.clip.learning.domain.repository.UserWordRepository

@Service
class AddUserWordCommandHandler(
  private val userWordRepository: UserWordRepository,
  private val exclusions: UserWordExclusionRepository,
  private val dictionaryExternal: DictionaryExternal,
  private val eventPublisher: ApplicationEventPublisher,
) {
  @Transactional
  fun handle(command: AddUserWordCommand) {
    val cleanTerm = command.lemma.lowercase().trim()

    // 1. Buscamos el lemma y la entrada oficial simultáneamente
//    val official = dictionaryExternal.determineLemmaInfo(cleanTerm)
    // 1. Fetch FULL context (Lemma + Translations + ILI) in one fast call
    // Your DictionaryExternal should now return a DTO with targetLemma, targetId, etc.
    val officialData = dictionaryExternal.findFullDefinition(cleanTerm)

    // NEW: If not found officially, try to get info from AI
//    val aiData = if (officialData == null) dictionaryExternal.generateAiSourceAndTargetLemma(cleanTerm) else null

    val lemma = officialData?.sourceLemma ?: cleanTerm
    val sourceId = officialData?.sourceLexicalEntryId // El UUID del diccionario principal
    val targetId = officialData?.targetLexicalEntryId

    val finalTargetLemma = officialData?.targetLemma ?: ""

    val finalTargetGloss = officialData?.targetGloss ?: "No definition found. Added manually."

    exclusions.deleteExclusion(command.userId, lemma)

    val existing = userWordRepository.findByUserIdAndLemma(command.userId, lemma)

    val saved = if (existing != null) {
      existing.apply {
        status = command.status
        targetLemma = finalTargetLemma

        targetGloss = finalTargetGloss
        sourceGloss = officialData?.sourceGloss

        sourceForms = officialData?.sourceForms
        targetForms = officialData?.targetForms

        // Actualizamos el ID por si antes era null y ahora existe en el diccionario
        sourceLexicalEntryId = sourceId
        targetLexicalEntryId = targetId
      }
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
        targetGloss = finalTargetGloss,
        sourceForms = officialData?.sourceForms,
        targetForms = officialData?.targetForms,
        // Es manual solo si NO se encontró en el diccionario oficial
        isManualLexicalEntry = (sourceId == null),
      )
      userWordRepository.save(newWord)
    }
    if(officialData?.targetLemma.isNullOrEmpty()) {
      eventPublisher.publishEvent(EnrichUserWordWithAiEvent(saved.id, saved.lemma))
    }
  }
}
