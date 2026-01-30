package sv.com.clip.learning.infrastructure.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "user_word_exclusions")
class UserWordExclusionEntity(
  @Id
  val id: UUID,
  val userId: UUID,
  @Column(nullable = false)
  val lemma: String, // store always as lowercase
) {}
