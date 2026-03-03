package sv.com.clip.shared.commands

interface CommandHandler<C> {
  fun handle(command: C)
}
