package sv.com.clip.learning.web

import jakarta.validation.Valid
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sv.com.clip.config.CustomUserDetails
import sv.com.clip.learning.application.usecases.AddUserWordExclusionCommandHandler
import sv.com.clip.learning.application.usecases.RemoveUserWordExclusionCommandHandler
import sv.com.clip.learning.domain.commands.AddUserWordExclusionCommand
import sv.com.clip.learning.domain.commands.RemoveUserWordExclusionCommand
import sv.com.clip.learning.infrastructure.rest.dto.ExcludeRequest
import java.util.UUID

@RestController
@RequestMapping("/learning/exclusions")
class ExclusionController(
  private val addExclusionHandler: AddUserWordExclusionCommandHandler,
  private val removeHandler: RemoveUserWordExclusionCommandHandler,
) {

  private val currentUserId: UUID
    get() = (SecurityContextHolder.getContext().authentication?.principal as CustomUserDetails).id

  @PostMapping
  fun excludeTerm(@Valid @RequestBody request: ExcludeRequest) {
    addExclusionHandler.handle(AddUserWordExclusionCommand(currentUserId, request.term))
  }

  @DeleteMapping
  fun deleteExclusion(@Valid @RequestBody request: ExcludeRequest) {
    removeHandler.handle(RemoveUserWordExclusionCommand(currentUserId, request.term))
  }
}
