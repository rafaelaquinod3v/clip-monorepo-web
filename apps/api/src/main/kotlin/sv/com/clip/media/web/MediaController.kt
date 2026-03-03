package sv.com.clip.media.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import sv.com.clip.config.CustomUserDetails
import sv.com.clip.media.application.usecases.UploadMediaContentCommandHandler
import sv.com.clip.media.domain.commands.UploadMediaContentCommand

@RestController
@RequestMapping("/media-content")
class MediaController(
  private val uploadMediaContentCommandHandler: UploadMediaContentCommandHandler,
) {

  @PostMapping("/upload")
  fun uploadMediaContent(@AuthenticationPrincipal user: CustomUserDetails, @RequestParam("file") file: MultipartFile): ResponseEntity<String> {

    if (file.isEmpty) {
      return ResponseEntity("El archivo está vacío", HttpStatus.BAD_REQUEST)
    }

    uploadMediaContentCommandHandler.handle(UploadMediaContentCommand(user.id, file.bytes, file.originalFilename))

    return ResponseEntity("Media Content subido con éxito", HttpStatus.CREATED)
  }
}
