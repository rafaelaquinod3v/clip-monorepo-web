package sv.com.clip.library.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.annotation.PostConstruct
import nl.siegmann.epublib.domain.TOCReference
import org.jsoup.Jsoup
import nl.siegmann.epublib.epub.EpubReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import sv.com.clip.media.api.MediaApi
import sv.com.clip.media.api.MediaContentMetadataRequest
import sv.com.clip.storage.api.StorageApi
import sv.com.clip.text.api.TextProcessorExternal
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths


@Service
class ImportEpubService(
  @Value("\${storage.location}") private val storageLocation: String,
  private val textProcessorService: TextProcessorExternal,
  private val storage: StorageApi,
  private val media: MediaApi,
) {

  private val root = Paths.get(storageLocation)
  private val mapper = jacksonObjectMapper()

  fun String.cleanForTTS(): String {
    return this
      // 1. Normalizar comillas elegantes/inclinadas
      .replace("[\u201C\u201D\u201E\u201F\u2033\u2036]".toRegex(), "\"")
      // 2. Normalizar apóstrofes
      .replace("[\u2018\u2019\u201A\u201B\u2032\u2035]".toRegex(), "'")
      // 3. Reemplazar guiones largos (em-dash) por comas o puntos para mejorar la entonación
      .replace("\u2014", ", ")
      // 4. Eliminar caracteres especiales no deseados (opcional)
      .replace("[#*|]".toRegex(), "")
      // 5. Normalizar espacios en blanco
      .replace("\\s+".toRegex(), " ")
      .trim()
  }

  @PostConstruct
  fun init() {
    // Crea el directorio si no existe al arrancar la app
    Files.createDirectories(root)
  }

  fun save(file: MultipartFile): String {
      val bytes = file.bytes
      val originalFilename = file.originalFilename

      val fileName = storage.store(bytes)
      val metadata = processEpubToJsonl(fileName)
      media.save(bytes, fileName, originalFilename, metadata)

      return fileName
  }


/*  private val BANNED_TITLES = setOf(
    // Front matter (Inicio)
    "copyright", "title page", "table of contents", "contents",
    "dedication", "praise", "introduction", "preface", "foreword",
    "prologue", "maps", "cast of characters", "note to the reader",

    // Back matter (Final)
    "epilogue", "afterword", "acknowledgments", "acknowledgements",
    "about the author", "about the illustrator", "bibliography",
    "further reading", "index", "glossary", "discussion questions",
    "sneak peek", "preview", "excerpt"
  )*/

/*  fun processEpubToJsonl(filePath: String) {
    // 1. Manejo limpio de rutas usando Path
    val epubPath = root.resolve(filePath)
    val epubFile = epubPath.toFile()

    // Crear la ruta del JSONL quitando la extensión .epub si quieres
    val jsonlName = epubPath.fileName.toString().replace(Regex("\\.epub$", RegexOption.IGNORE_CASE), "") + ".jsonl"
    val jsonlFile = root.resolve(jsonlName).toFile()

    jsonlFile.parentFile.mkdirs()

    // 2. Leer el libro
    val book = EpubReader().readEpub(epubFile.inputStream())

    var globalIndex = 0
    val spine = book.spine.spineReferences
    val totalChapters = spine.size

    FileOutputStream(jsonlFile).bufferedWriter().use { writer ->
      spine.forEachIndexed { chIdx, ref ->

        // FILTRO 1: Ignorar por posición (Skip técnico)
        // Normalmente los primeros 2 y últimos 2 archivos son portadas/legales
*//*        if (chIdx < 2 || chIdx > totalChapters - 3) {
          // Opcional: puedes loguear que estás saltando estos
          return@forEachIndexed
        }*//*

        val resource = ref.resource

        //val chapterTitle = findTitleInTOC(book.tableOfContents.tocReferences, resource.id)
         // ?: "Chapter ${chIdx + 1}"
        val chapterTitle = findTitleInTOC(book.tableOfContents.tocReferences, resource.id)
          ?.lowercase() ?: "Chapter ${chIdx + 1}"
        // FILTRO 2: Ignorar por Título

*//*        if (BANNED_TITLES.any { chapterTitle.lowercase().contains(it) }) {
          return@forEachIndexed
        }*//*
        // 1. Salto por palabras clave en el título
        if (BANNED_TITLES.any { chapterTitle.contains(it) }) {
          return@forEachIndexed
        }

        // 2. Salto por metadatos del recurso (a veces el ID del archivo dice 'copyright.xhtml')
        val resourceId = resource.id.lowercase()
        if (resourceId.contains("copyright") || resourceId.contains("cover")) {
          return@forEachIndexed
        }
        val htmlContent = String(resource.data, charset(resource.inputEncoding ?: "UTF-8"))

        val doc = Jsoup.parse(htmlContent)
        // FILTRO 3: Limpieza de selectores HTML comunes que no son texto
        doc.select("footer, header, .copyright, .nav").remove()
        val cleanText = doc.text()

        // FILTRO 4: Ignorar si el texto es demasiado corto (ej. solo una página de créditos)
        if (cleanText.length < 200) return@forEachIndexed

        val sentences = textProcessorService.splitBySentence(cleanText)

        sentences.forEachIndexed { sIdx, sentence ->
          if (sentence.isNotBlank()) {
            val entry = SentenceEntry(chapterTitle, sentence.trim().cleanForTTS(), globalIndex)
            writer.write(mapper.writeValueAsString(entry))
            writer.newLine()
            globalIndex++
          }
        }
      }
    }
  }*/
// ── Títulos prohibidos expandido ──
private val BANNED_TITLES = setOf(
  "copyright", "credits", "legal", "license", "licencia",
  "derechos", "imprint", "colophon", "about the author", "sobre el autor",
  "about the book", "acknowledgment", "acknowledgement", "agradecimiento",
  "dedication", "dedicatoria", "epigraph", "epígrafe",
  "table of contents", "contenido", "índice", "index",
  "cover", "portada", "title page", "half title",
  "also by", "otras obras", "by the same author",
  "bibliography", "bibliografía", "references", "further reading",
  "glossary", "glosario", "appendix", "apéndice", "notes", "notas",
  "foreword", "prólogo", "preface", "prefacio", "introduction", "introducción",
  "afterword", "epílogo", "epilogue"
)

  // ── Patrones de texto que delatan copyright/legal ──
  private val COPYRIGHT_PATTERNS = listOf(
    Regex("""copyright\s*(©|\(c\)|\d{4})""", RegexOption.IGNORE_CASE),
    Regex("""©\s*\d{4}"""),
    Regex("""all rights reserved""", RegexOption.IGNORE_CASE),
    Regex("""todos los derechos reservados""", RegexOption.IGNORE_CASE),
    Regex("""isbn[\s:\-]?[\d\-X]{9,}""", RegexOption.IGNORE_CASE),
    Regex("""published by\s+\w+""", RegexOption.IGNORE_CASE),
    Regex("""first published in""", RegexOption.IGNORE_CASE),
    Regex("""no part of this (publication|book|work)""", RegexOption.IGNORE_CASE),
    Regex("""without (written |prior )?permission""", RegexOption.IGNORE_CASE),
    Regex("""library of congress""", RegexOption.IGNORE_CASE),
    Regex("""printed in \w+""", RegexOption.IGNORE_CASE),
    Regex("""www\.[a-z0-9\-]+\.[a-z]{2,}""", RegexOption.IGNORE_CASE),
    Regex("""https?://"""),
    Regex("""this is a work of fiction""", RegexOption.IGNORE_CASE),
    Regex("""any resemblance to (actual|real) (persons?|events?)""", RegexOption.IGNORE_CASE),
    Regex("""typeset (by|in)\s+\w+""", RegexOption.IGNORE_CASE),
    Regex("""cover design (by)?""", RegexOption.IGNORE_CASE),
    Regex("""for information.{0,40}address""", RegexOption.IGNORE_CASE),
    Regex("""manufactured in""", RegexOption.IGNORE_CASE),
    Regex("""translation (by|copyright)""", RegexOption.IGNORE_CASE),
  )

  // ── Atributos epub:type que deben ignorarse ──
  private val BANNED_EPUB_TYPES = setOf(
    "copyright-page", "titlepage", "halftitlepage",
    "cover", "frontmatter", "backmatter",
    "toc", "landmarks", "loi", "lot",
    "acknowledgments", "colophon", "dedication",
    "epigraph", "foreword", "preface", "introduction",
    "afterword", "bibliography", "index", "glossary"
  )

  // ── IDs/hrefs de archivo que suelen ser no-contenido ──
  private val BANNED_RESOURCE_PATTERNS = listOf(
    Regex("""copyright""", RegexOption.IGNORE_CASE),
    Regex("""cover""", RegexOption.IGNORE_CASE),
    Regex("""toc|table.?of.?content""", RegexOption.IGNORE_CASE),
    Regex("""front.?matter""", RegexOption.IGNORE_CASE),
    Regex("""back.?matter""", RegexOption.IGNORE_CASE),
    Regex("""colophon""", RegexOption.IGNORE_CASE),
    Regex("""legal|license|licence""", RegexOption.IGNORE_CASE),
    Regex("""imprint""", RegexOption.IGNORE_CASE),
    Regex("""title.?page""", RegexOption.IGNORE_CASE),
    Regex("""dedication""", RegexOption.IGNORE_CASE),
    Regex("""nav""", RegexOption.IGNORE_CASE),
  )

  fun processEpubToJsonl(fileName: String) : MediaContentMetadataRequest {
    val epubPath = root.resolve(fileName)
    val epubFile = epubPath.toFile()
    val jsonlName = "$fileName.jsonl"
    val jsonlFile = root.resolve(jsonlName).toFile()
    jsonlFile.parentFile.mkdirs()

    val book = EpubReader().readEpub(epubFile.inputStream())

    val title = book.title?.takeIf { it.isNotBlank() } ?: ""
    val author = book.metadata.authors.firstOrNull()?.let {
      listOfNotNull(
        it.firstname?.takeIf { n -> n.isNotBlank() },
        it.lastname?.takeIf { n -> n.isNotBlank() }
      ).joinToString(" ")
    }?.takeIf { it.isNotBlank() } ?: ""

/*    media.updateMetadata(mediaId, MediaContentMetadataRequest(
      "EPUB",
      mapOf("title" to title, "author" to author)
    ))*/

    var globalIndex = 0
    val spine = book.spine.spineReferences

    FileOutputStream(jsonlFile).bufferedWriter().use { writer ->
      spine.forEachIndexed { chIdx, ref ->
        val resource = ref.resource
        val resourceId = resource.id.lowercase()
        val resourceHref = (resource.href ?: "").lowercase()

        // ── CAPA 1: Filtro por ID/href del recurso ──
        if (BANNED_RESOURCE_PATTERNS.any { it.containsMatchIn(resourceId) ||
            it.containsMatchIn(resourceHref) }) {
          return@forEachIndexed
        }

        // ── CAPA 2: Filtro por epub:type en el HTML ──
        val htmlContent = String(resource.data, charset(resource.inputEncoding ?: "UTF-8"))
        val doc = Jsoup.parse(htmlContent)

        val epubType = (
          doc.selectFirst("[epub\\:type]")?.attr("epub:type") ?:
          doc.body()?.attr("epub:type") ?: ""
          ).lowercase()

        if (BANNED_EPUB_TYPES.any { epubType.contains(it) }) {
          return@forEachIndexed
        }

        // ── CAPA 3: Filtro por título del capítulo ──
        val chapterTitle = findTitleInTOC(book.tableOfContents.tocReferences, resource.id)
          ?.lowercase() ?: "chapter ${chIdx + 1}"

        if (BANNED_TITLES.any { chapterTitle.contains(it) }) {
          return@forEachIndexed
        }

        // ── CAPA 4: Limpieza del DOM ──
        doc.select("""
                script, style, nav, footer, header, aside, figure, figcaption,
                .copyright, .colophon, .legal, .toc, .nav, .cover,
                .frontmatter, .backmatter, .imprint, .credits,
                [epub\:type~=copyright-page],
                [epub\:type~=toc],
                [epub\:type~=cover]
            """.trimIndent()).remove()

        val cleanText = doc.body()?.text() ?: doc.text()

        // ── CAPA 5: Filtro por longitud mínima ──
        if (cleanText.length < 200) return@forEachIndexed

        // ── CAPA 6: Filtro por densidad de patrones copyright ──
        if (hasCopyrightDensity(cleanText)) return@forEachIndexed

        // ── Extracción de oraciones ──
        val sentences = textProcessorService.splitBySentence(cleanText)
        sentences.forEachIndexed { _, sentence ->
          if (sentence.isNotBlank()) {
            val entry = SentenceEntry(
              chapterTitle,
              sentence.trim().cleanForTTS(),
              globalIndex
            )
            writer.write(mapper.writeValueAsString(entry))
            writer.newLine()
            globalIndex++
          }
        }
      }
    }

    return MediaContentMetadataRequest(
      "EPUB",
      mapOf("title" to title, "author" to author),
    )
  }

  // Devuelve true si el bloque tiene demasiados indicios de copyright
  private fun hasCopyrightDensity(text: String): Boolean {
    val matches = COPYRIGHT_PATTERNS.count { it.containsMatchIn(text) }
    // Si 2 o más patrones coinciden, es casi seguro texto legal
    return matches >= 2
  }
  private fun findTitleInTOC(references: List<TOCReference>, resourceId: String): String? {
    for (ref in references) {
      if (ref.resourceId == resourceId) return ref.title
      val childTitle = findTitleInTOC(ref.children, resourceId)
      if (childTitle != null) return childTitle
    }
    return null
  }



}
