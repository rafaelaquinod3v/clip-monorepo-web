package sv.com.clip.storage.api

import org.springframework.web.multipart.MultipartFile

interface StorageExternal {
  fun store(file: MultipartFile): String
}
