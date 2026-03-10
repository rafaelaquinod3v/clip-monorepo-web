package sv.com.clip.config

import com.k2fsa.sherpa.onnx.OfflineRecognizerResult
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import sv.com.clip.asr.domain.AsrPort
import sv.com.clip.tts.domain.TtsPort

@TestConfiguration
class TestConfig {
  @Bean
  @Primary
  fun ttsPort(): TtsPort =  TtsPort { _, _, outputStream ->
    outputStream.close()
  }

  @Bean @Primary
  fun asrPort(): AsrPort = AsrPort { _, _ ->
    OfflineRecognizerResult(null, null, null, null, null, null)
  }
}
