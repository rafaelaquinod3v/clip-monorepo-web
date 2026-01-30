package sv.com.clip.dictionary.api

import java.util.UUID

data class FullWordContextDTO(
  val sourceLemma: String,
  val targetLemmaAndForms: String?,    // Target Lemma via ILI
  val sourceLexicalEntryId: UUID,
  val targetLexicalEntryId: UUID?,         // Target ID via ILI
  val targetGloss: String?,
)
