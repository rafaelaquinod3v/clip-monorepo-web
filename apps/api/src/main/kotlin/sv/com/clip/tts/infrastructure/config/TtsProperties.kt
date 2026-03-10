package sv.com.clip.tts.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "clip.tts")
data class TtsProperties(
  val baseUrl: String,
  val connectTimeoutSeconds: Long = 10,
  val readTimeoutSeconds: Long = 60
)
