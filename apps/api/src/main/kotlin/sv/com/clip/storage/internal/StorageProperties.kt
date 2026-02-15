package sv.com.clip.storage.internal

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "storage")
data class StorageProperties(
  var location: String = "upload-dir"
)
