package sv.com.clip.dictionary.infrastructure.persistence.jpa

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import org.springframework.data.domain.Persistable
import sv.com.clip.dictionary.domain.model.Sense
import sv.com.clip.dictionary.domain.model.SenseId
import java.util.UUID

@Entity
@Table(name = "lexical_entry_senses")
class SenseEntity(
    @Id
    private val id: UUID,
    var sourceId: String? = null,

  // DDD: Referencia a otro agregado por ID (Concept.ili)
  // Suponiendo que ConceptIli es otra value class o un String (iliId)
    @Column(name = "concept_ili", nullable = false)
  val conceptIli: String, //TODO: add index to DB

    @Column(columnDefinition = "TEXT")
  val gloss: String? = null,
    @Column(columnDefinition = "TEXT")
  val definition: String? = null,
  // Mapeo de Value Objects (Ejemplos)
    @ElementCollection
  @CollectionTable(name = "sense_examples", joinColumns = [JoinColumn(name = "sense_id")])
  private val _examples: MutableList<UsageExampleEmbeddable> = mutableListOf(),

  // Relación con la raíz del agregado
    @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lexical_entry_id")
  var lexicalEntryEntity: LexicalEntryEntity? = null
) : Persistable<UUID> {
  val examples: List<UsageExampleEmbeddable> get() = _examples.toList()
  fun toDomain(): Sense {
    return Sense(
      id = SenseId(this.id),
      sourceId = this.sourceId,
      conceptIli = this.conceptIli,
      gloss = this.gloss,
      definition = this.definition,
      examples = this._examples.map { it.toDomain() },
    )
  }
  companion object {
    fun fromDomain(sense: Sense) : SenseEntity {
      return SenseEntity(
        id = sense.id.value,
        sourceId = sense.sourceId,
        conceptIli = sense.conceptIli,
        gloss = sense.gloss,
        definition = sense.definition,
        _examples = sense.examples.map { UsageExampleEmbeddable.fromDomain(it) }.toMutableList(),
      )
    }
  }

  @Transient
  private var isNewEntity: Boolean = true

  // --- Implementación de Persistable ---

  override fun getId(): UUID = id

  override fun isNew(): Boolean = isNewEntity

  // Metodo para marcar que la entidad ya existe (usado al cargar de DB)
  @PostLoad
  @PostPersist
  fun markNotNew() {
    this.isNewEntity = false
  }
}
