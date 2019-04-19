package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.utilities.Optional

interface CommandHandler {
    fun handle(command: Command): Result

    sealed class Result {
        object NotHandled : Result()
        data class Handled(val newEvent: Optional<Event>) : Result()
    }
}