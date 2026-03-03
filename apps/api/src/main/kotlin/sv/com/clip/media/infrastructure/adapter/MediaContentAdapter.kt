package sv.com.clip.media.infrastructure.adapter

import org.springframework.stereotype.Repository
import sv.com.clip.media.domain.model.MediaContent
import sv.com.clip.media.domain.model.MediaType
import sv.com.clip.media.domain.repository.MediaContentRepository
import sv.com.clip.media.infrastructure.persistence.entity.MediaContentEntity
import sv.com.clip.media.infrastructure.persistence.repository.JpaMediaContentRepository
import sv.com.clip.shared.pagination.PageQuery
import sv.com.clip.shared.pagination.toPageable
import java.util.UUID

@Repository
class MediaContentAdapter(
  private val jpaRepository: JpaMediaContentRepository
) : MediaContentRepository {
  override fun save(media: MediaContent): MediaContent {
    return jpaRepository
      .save(MediaContentEntity.fromDomain(media))
      .toDomain()
  }

  override fun findById(id: UUID): MediaContent? {
    return jpaRepository.findById(id).get().toDomain()
  }

  override fun findByIdAndUserId(
    id: UUID,
    requestingUserId: UUID
  ): MediaContent? {
    return jpaRepository.findByIdAndUserId(id, requestingUserId)?.toDomain()
  }

  override fun findAllByUserIdAndMediaType(
    userId: UUID,
    mediaType: MediaType
  ): List<MediaContent> {
    return jpaRepository.findAllByUserIdAndMediaType(userId, mediaType).map { it.toDomain() }
  }

  override fun findAllByUserIdAndMediaTypePageable(
    userId: UUID,
    mediaType: MediaType,
    pageQuery: PageQuery
  ): List<MediaContent> {
    return jpaRepository.findAllByUserIdAndMediaType(userId, mediaType, pageQuery.toPageable()).map { it.toDomain() }.content
  }

  override fun findAllByUserIdAndMediaTypeInPageable(
    userId: UUID,
    mediaTypes: Collection<MediaType>,
    pageQuery: PageQuery
  ): List<MediaContent> {
    return jpaRepository.findAllByUserIdAndMediaTypeIn(userId, mediaTypes, pageQuery.toPageable()).map { it.toDomain() }.content
  }
}
