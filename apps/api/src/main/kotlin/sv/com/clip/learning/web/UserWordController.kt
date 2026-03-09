package sv.com.clip.learning.web

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sv.com.clip.learning.application.usecases.AddUserWordCommandHandler
import sv.com.clip.learning.application.usecases.RemoveUserWordCommandHandler
import sv.com.clip.learning.application.usecases.UpdateUserWordStatusCommandHandler
import sv.com.clip.learning.domain.WordStatus
import sv.com.clip.learning.domain.commands.AddUserWordCommand
import sv.com.clip.learning.domain.commands.RemoveUserWordCommand
import sv.com.clip.learning.domain.commands.UpdateUserWordStatusCommand
import sv.com.clip.learning.infrastructure.rest.dto.UserWordRequest
import sv.com.clip.user.api.CurrentUserId
import java.util.UUID

@RestController
@RequestMapping("/learning/user-words")
class UserWordController(
  private val addHandler: AddUserWordCommandHandler,
  private val removeHandler: RemoveUserWordCommandHandler,
  private val updateHandler: UpdateUserWordStatusCommandHandler,
) {

  @PostMapping
  fun addUserWord(@CurrentUserId userId: UUID, @Valid @RequestBody request: UserWordRequest) {
    val command = AddUserWordCommand(
      userId,
      request.term,
      WordStatus.fromCode(request.statusCode),
      )
    addHandler.handle(command)
  }

  @PatchMapping
  fun patchUserWord(@CurrentUserId userId: UUID, @Valid @RequestBody request: UserWordRequest) {
    val command = UpdateUserWordStatusCommand(
      userId,
      request.term,
      WordStatus.fromCode(request.statusCode),
    )
    updateHandler.handle(command)
  }

  @DeleteMapping
  fun deleteUserWord(@CurrentUserId userId: UUID, @Valid @RequestBody request: UserWordRequest) {
    val command = RemoveUserWordCommand(
      userId = userId,
      term = request.term,
    )
    removeHandler.handle(command)
  }
}
