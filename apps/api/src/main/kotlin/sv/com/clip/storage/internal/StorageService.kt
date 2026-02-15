package sv.com.clip.storage.internal

import org.springframework.web.multipart.MultipartFile
import sv.com.clip.storage.api.StorageExternal
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

class StorageService(properties: StorageProperties) : StorageExternal {
  private val rootLocation = Paths.get(properties.location)

  fun init() {
    Files.createDirectories(rootLocation)
  }

  override fun store(file: MultipartFile): String {
    if (file.isEmpty) throw RuntimeException("No se puede guardar un archivo vacío.")

    // 1. Generar un nombre único para evitar colisiones
    val extension = file.originalFilename?.substringAfterLast(".", "")
    val fileName = "${UUID.randomUUID()}.$extension"

    // 2. Resolver la ruta de destino
    val destinationFile = rootLocation.resolve(Paths.get(fileName))
      .normalize().toAbsolutePath()

    // 3. Guardar el archivo en el disco
    file.inputStream.use { inputStream ->
      Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING)
    }

    return fileName // Devolvemos el nombre para guardarlo en la DB
  }
}
