package sv.com.clip.library.application.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import sv.com.clip.library.application.SentenceEntry
import sv.com.clip.shared.tts.cleanForTTS
import sv.com.clip.text.api.TextProcessorExternal
import java.io.FileOutputStream
import java.nio.file.Paths

@Service
class ImportPdfService(
  @Value("\${storage.location}") private val storageLocation: String,
  private val textProcessorService: TextProcessorExternal,
) {
  private val root = Paths.get(storageLocation)
  private val mapper = jacksonObjectMapper()

  fun generateJsonl(fileName: String) {
    val pdfPath = root.resolve(fileName)
    val pdfFile = pdfPath.toFile()
    val jsonlName = "$fileName.jsonl"
    val jsonlFile = root.resolve(jsonlName).toFile()
    jsonlFile.parentFile.mkdirs()

    var globalIndex = 0

    Loader.loadPDF(pdfFile).use { document ->
      val stripper = PDFTextStripper()

      FileOutputStream(jsonlFile).bufferedWriter().use { writer ->
        for (pageNum in 1..document.numberOfPages) {
          stripper.startPage = pageNum
          stripper.endPage = pageNum

          val rawText = stripper.getText(document)
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()

          // ── Filtro por longitud mínima ──
          if (rawText.length < 200) continue

          // ── Filtro copyright ──
          //if (hasCopyrightDensity(rawText)) continue

          // ── Extracción de oraciones ──
          val sentences = textProcessorService.splitBySentence(rawText)

          sentences.forEach { sentence ->
            if (sentence.isNotBlank()) {
              val entry = SentenceEntry(
                "page_$pageNum",
                text = sentence.trim().cleanForTTS(),
                index = globalIndex
              )
              writer.write(mapper.writeValueAsString(entry))
              writer.newLine()
              globalIndex++
            }
          }
        }
      }
    }
  }
}
