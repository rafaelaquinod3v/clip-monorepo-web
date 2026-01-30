package sv.com.clip.dictionary.api

import java.util.UUID

data class WordTranslationDTO(val id: UUID, val sourceLemma: String, val targetLemma: String?)
