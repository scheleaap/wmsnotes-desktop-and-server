package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.api.GrpcCommandMapper
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import io.grpc.Deadline
import javax.inject.Inject


class CompensatingActionExecutor @Inject constructor(
        private val remoteCommandService: CommandServiceGrpc.CommandServiceBlockingStub,
        private val grpcDeadline: Deadline?,
        private val eventToCommandMapper: EventToCommandMapper,
        private val grpcCommandMapper: GrpcCommandMapper,
        private val commandProcessor: CommandProcessor
) {
    fun execute(compensatingAction: CompensatingAction): ExecutionResult {
        TODO()
    }

    data class ExecutionResult(
            val success: Boolean,
            val newLocalEvents: List<EventIdAndRevision>,
            val newRemoteEvents: List<EventIdAndRevision>
    )

    data class EventIdAndRevision(val eventId: Int, val revision: Int) {
        constructor (event: Event) : this(event.eventId, event.revision)
    }
}
