package sv.com.clip.learning.domain

import org.jmolecules.ddd.annotation.AggregateRoot
import java.util.UUID

@AggregateRoot
class UserWordExclusion(
  val id: UUID,
  val userId: UUID,
  val lemma: String, // Example: "Google", "Paris", "Rodrigo"
) {}
