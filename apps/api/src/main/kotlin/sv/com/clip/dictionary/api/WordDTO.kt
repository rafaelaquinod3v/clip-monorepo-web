package sv.com.clip.dictionary.api

import java.util.UUID

data class WordDTO(val id: UUID, val term: String, val definition: String?)
