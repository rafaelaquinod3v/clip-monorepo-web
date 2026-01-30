package sv.com.clip.learning.web

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sv.com.clip.learning.application.AnalysisResult
import sv.com.clip.learning.application.TextAnalysisService
import sv.com.clip.learning.application.WordAnalysis
import java.util.UUID

@RestController
@RequestMapping("/text-analysis")
class TextAnalysisController(
  private val textAnalysisService: TextAnalysisService
) {
  @PostMapping
  fun analyzeText(@RequestBody rawText: String) : AnalysisResult {
    return textAnalysisService.analyzeText(UUID.randomUUID(), rawText)
  }

  @PostMapping("/analyze-single-word")
  fun analyzeWord(@RequestBody rawText: String) : WordAnalysis {
    return textAnalysisService.analyzeSingleWord(UUID.randomUUID(), rawText)
  }
}
