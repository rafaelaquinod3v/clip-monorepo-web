package sv.com.clip.learning.domain

import org.jmolecules.ddd.annotation.AggregateRoot
import java.util.UUID

@AggregateRoot
class UserWord(
  val id: UUID = UUID.randomUUID(),
  val userId: UUID,
  val lemma: String,
  var status: WordStatus = WordStatus.NEW,
  var isManualLexicalEntry: Boolean = false, // si esto es verdadero, se deben llenar los siguientes
  var targetLemmaAndForms: String? = null,
  var targetGloss: String? = null,
  var sourceLexicalEntryId: UUID? = null, // referencia ligera (id) al dictionary
  var targetLexicalEntryId: UUID? = null,
) {
  fun upgradeStatus() {
    this.status = when (status) {
      WordStatus.NEW -> WordStatus.RECOGNIZED
      WordStatus.RECOGNIZED -> WordStatus.FAMILIAR
      WordStatus.FAMILIAR -> WordStatus.KNOWN
      WordStatus.KNOWN -> WordStatus.LEARNED
      else -> {
        WordStatus.valueOf(status.name)
      }
//      WordStatus.LEARNED -> WordStatus.LEARNED
//      WordStatus.NOT_FOUND -> WordStatus.NOT_FOUND
//      WordStatus.IGNORED -> WordStatus.IGNORED
    }
  }

}
