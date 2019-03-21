package info.maaskant.wmsnotes.client.synchronization.commandexecutor

import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.Event

interface CommandExecutor {
    fun execute(command: Command): ExecutionResult

    sealed class ExecutionResult {
        object Failure : ExecutionResult()
        data class Success(val newEventMetadata: EventMetadata?) : ExecutionResult()
    }

    data class EventMetadata(val eventId: Int, val aggId: String, val revision: Int) {
        constructor (event: Event) : this(event.eventId, event.aggId, event.revision)
    }
}
