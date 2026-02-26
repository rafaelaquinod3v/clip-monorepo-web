package sv.com.clip.media.infrastructure.adapter

import org.springframework.stereotype.Repository
import sv.com.clip.media.domain.model.MediaContent
import sv.com.clip.media.domain.repository.MediaContentRepository
import sv.com.clip.media.infrastructure.persistence.entity.MediaContentEntity
import sv.com.clip.media.infrastructure.persistence.repository.JpaMediaContentRepository

@Repository
class MediaContentAdapter(
  private val jpaRepository: JpaMediaContentRepository
) : MediaContentRepository {
  override fun save(media: MediaContent): MediaContent {
    return jpaRepository
      .save(MediaContentEntity.fromDomain(media))
      .toDomain()
  }
}
