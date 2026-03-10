package sv.com.clip.asr.api

import com.k2fsa.sherpa.onnx.OfflineRecognizerResult

interface AsrProvider {
  fun getTimestampsFromAudio(samples: FloatArray, sampleRate: Float): OfflineRecognizerResult
}
