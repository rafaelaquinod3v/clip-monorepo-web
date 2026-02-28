package sv.com.clip.media.infrastructure.persistence.config

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import sv.com.clip.media.domain.model.*

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = EpubMediaContentMetadata::class,  name = "EPUB"),
  JsonSubTypes.Type(value = PdfMediaContentMetadata::class,   name = "PDF"),
  JsonSubTypes.Type(value = AudioMediaContentMetadata::class, name = "AUDIO"),
  JsonSubTypes.Type(value = VideoMediaContentMetadata::class, name = "VIDEO"),
)
abstract class MediaContentMetadataMixin()
