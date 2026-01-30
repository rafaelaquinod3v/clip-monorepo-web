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
  @Column(columnDefinition = "TEXT")
  var lemma: String,
  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20)
  var status: WordStatus,
  var isManualLexicalEntry: Boolean = false,
  @Column(columnDefinition = "TEXT")
  var targetLemma: String? = null,
  @Column(columnDefinition = "TEXT")
  var sourceGloss: String? = null,
  @Column(columnDefinition = "TEXT")
  var targetGloss: String? = null,
  var sourceLexicalEntryId: UUID? = null,
  var targetLexicalEntryId: UUID? = null,
  @Column(columnDefinition = "TEXT")
  var sourceForms: String? = null,
  @Column(columnDefinition = "TEXT")
  var targetForms: String? = null,
) {
  fun toDomain() : UserWord {
    return UserWord(
      this.id,
      this.userId,
      this.lemma,
      this.status,
      this.isManualLexicalEntry,
      this.targetLemma,
      this.sourceGloss,
      this.targetGloss,
      this.sourceLexicalEntryId,
      this.targetLexicalEntryId,
      this.sourceForms,
      this.targetForms,
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
        userWord.targetLemma,
        userWord.sourceGloss,
        userWord.targetGloss,
        userWord.sourceLexicalEntryId,
        userWord.targetLexicalEntryId,
        userWord.sourceForms,
        userWord.targetForms,
      )
    }
  }
}
