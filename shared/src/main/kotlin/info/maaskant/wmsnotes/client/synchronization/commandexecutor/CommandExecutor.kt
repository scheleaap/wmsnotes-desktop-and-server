package info.maaskant.wmsnotes.client.synchronization.commandexecutor

import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.CommandError
import info.maaskant.wmsnotes.model.Event

interface CommandExecutor {
    fun execute(command: Command, lastRevision: Int): ExecutionResult

    sealed class ExecutionResult {
        data class Failure(val error: CommandError) : ExecutionResult()
        data class Success(val newEventMetadata: EventMetadata?) : ExecutionResult()
    }

    data class EventMetadata(val eventId: Int, val aggId: String, val revision: Int) {
        constructor (event: Event) : this(event.eventId, event.aggId, event.revision)
    }
}
