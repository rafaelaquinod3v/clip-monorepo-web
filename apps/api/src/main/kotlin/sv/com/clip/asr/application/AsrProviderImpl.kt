package sv.com.clip.asr.application

import com.k2fsa.sherpa.onnx.OfflineRecognizerResult
import org.springframework.stereotype.Service
import sv.com.clip.asr.api.AsrProvider
import sv.com.clip.asr.domain.AsrPort

@Service
class AsrProviderImpl(
  private val asrAdapter: AsrPort,
) : AsrProvider {
  override fun getTimestampsFromAudio(
    samples: FloatArray,
    sampleRate: Float
  ): OfflineRecognizerResult {
    return asrAdapter.getTimestampsFromAudio(samples, sampleRate)
  }
}
