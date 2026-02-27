package sv.com.clip.storage.api

import org.springframework.web.multipart.MultipartFile

interface StorageApi {
  fun store(file: MultipartFile): String
  fun store(bytes: ByteArray): String
}
