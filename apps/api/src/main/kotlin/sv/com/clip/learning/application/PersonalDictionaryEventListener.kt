package sv.com.clip.learning.application

import org.springframework.stereotype.Component
import sv.com.clip.learning.domain.events.EnrichUserWordWithAiEvent
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.scheduling.annotation.Async
import sv.com.clip.dictionary.api.DictionaryExternal
import sv.com.clip.learning.domain.repository.UserWordRepository

@Component
class PersonalDictionaryEventListener(
  private val external: DictionaryExternal,
  private val userWordRepository: UserWordRepository,
) {

  @Async
  @ApplicationModuleListener
  fun onEnrichUserWordWithAiEvent(event: EnrichUserWordWithAiEvent) {
    val userWord = userWordRepository.findById(event.id) ?: return
    if (userWord.targetLemma.isNullOrBlank()) {
      val aiData = external.generateAiSourceAndTargetLemma(userWord.lemma)
      aiData?.let {
        userWord.lemma = it.sourceLemma?.lowercase()?.trim() ?: userWord.lemma
        userWord.targetLemma = it.targetLemma
        userWordRepository.save(userWord)
      }
    }
  }
}
