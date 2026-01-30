package sv.com.clip.dictionary.api

import java.util.UUID

data class FullWordContextDTO(
  val sourceLemma: String,
  val targetLemma: String?,    // Target Lemma via ILI
  val sourceLexicalEntryId: UUID,
  val targetLexicalEntryId: UUID?,         // Target ID via ILI
  val targetGloss: String?,
  val sourceGloss: String?,
  var targetForms: String? = null,
  var sourceForms: String? = null,
)
