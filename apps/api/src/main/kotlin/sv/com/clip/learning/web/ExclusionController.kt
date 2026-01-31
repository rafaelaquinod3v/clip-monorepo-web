package sv.com.clip.learning.web

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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
  @PostMapping
  fun excludeTerm(@Valid @RequestBody request: ExcludeRequest) {
    addExclusionHandler.handle(AddUserWordExclusionCommand(UUID.randomUUID(), request.term))
  }

  @DeleteMapping
  fun deleteExclusion(@Valid @RequestBody request: ExcludeRequest) {
    removeHandler.handle(RemoveUserWordExclusionCommand(UUID.randomUUID(), request.term))
  }
}
