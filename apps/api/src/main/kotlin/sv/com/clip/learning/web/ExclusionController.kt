package sv.com.clip.learning.web

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sv.com.clip.learning.application.usecases.AddUserWordExclusionCommandHandler
import sv.com.clip.learning.application.usecases.RemoveUserWordExclusionCommandHandler
import sv.com.clip.learning.domain.commands.AddUserWordExclusionCommand
import sv.com.clip.learning.domain.commands.RemoveUserWordExclusionCommand
import sv.com.clip.learning.infrastructure.rest.dto.ExcludeRequest
import sv.com.clip.user.api.CurrentUserId
import java.util.UUID

@RestController
@RequestMapping("/learning/exclusions")
class ExclusionController(
  private val addExclusionHandler: AddUserWordExclusionCommandHandler,
  private val removeHandler: RemoveUserWordExclusionCommandHandler,
) {

  @PostMapping
  fun excludeTerm(@CurrentUserId userId: UUID, @Valid @RequestBody request: ExcludeRequest) {
    addExclusionHandler.handle(AddUserWordExclusionCommand(userId, request.term))
  }

  @DeleteMapping
  fun deleteExclusion(@CurrentUserId userId: UUID, @Valid @RequestBody request: ExcludeRequest) {
    removeHandler.handle(RemoveUserWordExclusionCommand(userId, request.term))
  }
}
