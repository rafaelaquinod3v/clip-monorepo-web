package sv.com.clip.media.infrastructure.persistence.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import sv.com.clip.media.domain.model.MediaType
import sv.com.clip.media.infrastructure.persistence.entity.MediaContentEntity
import java.util.UUID

interface JpaMediaContentRepository : JpaRepository<MediaContentEntity, UUID> {
  fun findByIdAndUserId(id: UUID, userId: UUID): MediaContentEntity?
  fun findAllByUserIdAndMediaType(userId: UUID, mediaType: MediaType): List<MediaContentEntity>
  fun findAllByUserIdAndMediaType(userId: UUID, mediaType: MediaType, pageable: Pageable): Page<MediaContentEntity>
  fun findAllByUserIdAndMediaTypeIn(userId: UUID, mediaTypes: Collection<MediaType>, pageable: Pageable): Page<MediaContentEntity>
}
