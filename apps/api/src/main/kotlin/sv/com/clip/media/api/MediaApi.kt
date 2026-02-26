package sv.com.clip.media.api

import java.util.UUID

interface MediaApi {
  fun save(media: MediaRequest) : UUID
}
