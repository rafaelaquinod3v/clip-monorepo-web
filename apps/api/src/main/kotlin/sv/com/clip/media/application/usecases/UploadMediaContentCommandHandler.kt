package sv.com.clip.media.application.usecases


import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import sv.com.clip.media.application.services.MediaService
import sv.com.clip.media.domain.commands.UploadMediaContentCommand
import sv.com.clip.media.domain.events.MediaContentUploadedEvent
import sv.com.clip.shared.commands.CommandHandler
import sv.com.clip.storage.api.StorageApi

@Component
class UploadMediaContentCommandHandler(
  private val storageApi: StorageApi,
  private val mediaService: MediaService,
  private val eventPublisher: ApplicationEventPublisher
) : CommandHandler<UploadMediaContentCommand> {

  @Transactional
  override fun handle(command: UploadMediaContentCommand) {
    val fileName = storageApi.store(command.file)
    val mediaId = mediaService.save(command.userId, command.file, fileName, command.originalFileName)
    eventPublisher.publishEvent(MediaContentUploadedEvent(command.userId, mediaId))
  }
}
