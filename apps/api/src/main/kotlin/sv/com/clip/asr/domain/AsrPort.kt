package sv.com.clip.asr.domain

import com.k2fsa.sherpa.onnx.OfflineRecognizerResult

fun interface AsrPort {
  fun getTimestampsFromAudio(samples: FloatArray, sampleRate: Float): OfflineRecognizerResult
}
