package sv.com.clip.learning.domain.events

data class TermsNotFoundEvent(
  val terms: Set<String>,
)
