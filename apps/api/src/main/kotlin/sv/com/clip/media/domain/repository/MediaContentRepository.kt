package sv.com.clip.media.domain.repository

import sv.com.clip.media.domain.model.MediaContent

interface MediaContentRepository {
  fun save(media: MediaContent) : MediaContent
}
