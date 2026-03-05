package sv.com.clip.storage.internal

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import sv.com.clip.storage.api.StorageApi
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class StorageService(properties: StorageProperties) : StorageApi {
  private val rootLocation = Paths.get(properties.location)

  @PostConstruct
  fun init() {
    Files.createDirectories(rootLocation)
  }

  override fun store(file: MultipartFile): String {
    try {
      if (file.isEmpty) throw RuntimeException("No se puede guardar un archivo vacío.")

      // 1. Generar un nombre único para evitar colisiones
      val extension = file.originalFilename?.substringAfterLast(".", "")
      val fileName = "${UUID.randomUUID()}.$extension"

      // 2. Resolver la ruta de destino
      val destinationFile = rootLocation.resolve(Paths.get(fileName)).normalize().toAbsolutePath()

      // 3. Guardar el archivo en el disco
      file.inputStream.use { inputStream ->
        Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING)
      }

      return fileName // Devolvemos el nombre para guardarlo en la DB
    } catch (e: Exception) {
      throw RuntimeException("Error al guardar el archivo: ${e.message}")
    }
  }

  override fun store(bytes: ByteArray): String {
    val fileName = UUID.randomUUID().toString()
    val path = rootLocation.resolve(Paths.get(fileName)).normalize().toAbsolutePath()
    if (!path.startsWith(rootLocation.toAbsolutePath())) {
      throw SecurityException("Path fuera del directorio permitido")
    }
    Files.write(path, bytes)
    return fileName
  }

  override fun load(fileName: String): ByteArray? {
    return try {
      val filePath = rootLocation.resolve(Paths.get(fileName)).normalize()
      if (!filePath.startsWith(rootLocation.toAbsolutePath())) {
        throw SecurityException("Path fuera del directorio permitido")
      }
      if(Files.exists(filePath) && Files.isReadable(filePath)) {
        Files.readAllBytes(filePath)
      }else {
        null
      }
    } catch (e: Exception) {
      throw RuntimeException("No se pudo leer el archivo: ${e.message}")
    }
  }
}
