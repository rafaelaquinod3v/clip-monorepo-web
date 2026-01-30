package sv.com.clip.learning.infrastructure.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import sv.com.clip.learning.domain.UserWord
import sv.com.clip.learning.domain.WordStatus
import java.util.UUID
import kotlin.String

@Entity
@Table(name = "user_words")
class UserWordEntity(
  @Id
  val id: UUID,
  val userId: UUID,
  val lemma: String,
  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20)
  var status: WordStatus,
  var isManualLexicalEntry: Boolean = false,
  var targetLemmaAndForms: String? = null,
  var targetGloss: String? = null,
  var sourceLexicalEntryId: UUID? = null,
  var targetLexicalEntryId: UUID? = null,
) {
  fun toDomain() : UserWord {
    return UserWord(
      this.id,
      this.userId,
      this.lemma,
      this.status,
      this.isManualLexicalEntry,
      this.targetLemmaAndForms,
      this.targetGloss,
      this.sourceLexicalEntryId,
      this.targetLexicalEntryId,
    )
  }
  companion object {
    fun fromDomain(userWord : UserWord) : UserWordEntity {
      return UserWordEntity(
        userWord.id,
        userWord.userId,
        userWord.lemma,
        userWord.status,
        userWord.isManualLexicalEntry,
        userWord.targetLemmaAndForms,
        userWord.targetGloss,
        userWord.sourceLexicalEntryId,
        userWord.targetLexicalEntryId,
      )
    }
  }
}
