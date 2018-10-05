package info.maaskant.wmsnotes.model.synchronization

import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.server.api.GrpcConverters
import info.maaskant.wmsnotes.server.command.grpc.Command
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import io.reactivex.Observable
import javax.inject.Inject

class Synchronizer @Inject constructor(
        val localEvents: ModifiableEventRepository,
        val remoteEvents: ModifiableEventRepository,
        val remoteCommandService: CommandServiceGrpc.CommandServiceBlockingStub,
        val remoteEventToLocalCommandMapper: RemoteEventToLocalCommandMapper,
        val commandProcessor: CommandProcessor
) {

    private val logger by logger()

    private val lastLocalRevisions: MutableMap<String, Int?> = HashMap<String, Int?>().withDefault { null }
    private val lastRemoteRevisions: MutableMap<String, Int?> = HashMap<String, Int?>().withDefault { null }

    fun synchronize() {
        val localOutboundEvents = localEvents.getCurrentEvents().toList().blockingGet()
        val remoteInboundEvents = remoteEvents.getCurrentEvents().toList().blockingGet()
        val localOutboundNoteIds = localOutboundEvents.map { it.noteId }
        val remoteInboundNoteIds = remoteInboundEvents.map { it.noteId }
        sendLocalEventsToServer(localOutboundEvents.filter { it.noteId !in remoteInboundNoteIds })
        processRemoteEvents(remoteInboundEvents.filter { it.noteId !in localOutboundNoteIds })
    }

    private fun sendLocalEventsToServer(localOutboundEvents: List<Event>) {
        val noteIdsWithErrors: MutableSet<String> = HashSet<String>()
        localOutboundEvents
                .stream() // Necessary for filtering to work
                .filter { it.noteId !in noteIdsWithErrors }
                .forEach {
                    logger.debug("Processing local event: $it")
                    val request = GrpcConverters.toGrpcPostCommandRequest(event = it, lastRevision = lastRemoteRevisions[it.noteId])
                    logger.debug("Sending command to server: $request")
                    val response = remoteCommandService.postCommand(request)
                    if (response.status == Command.PostCommandResponse.Status.SUCCESS) {
                        localEvents.removeEvent(it)
                        lastRemoteRevisions[it.noteId] = response.newRevision
                        logger.debug("Remote event successfully processed: $it")
                    } else {
                        noteIdsWithErrors += it.noteId
                        logger.debug("Command not processed by server: $request -> ${response.status} ${response.errorDescription}")
                    }
                }
    }

    private fun processRemoteEvents(remoteInboundEvents: List<Event>) {
        val noteIdsWithErrors: MutableSet<String> = HashSet<String>()
        remoteInboundEvents
                .stream() // Necessary for filtering to work
                .filter { it.noteId !in noteIdsWithErrors }
                .forEach {
                    logger.debug("Processing remote event: $it")
                    val command = remoteEventToLocalCommandMapper.map(it)
                    try {
                        val event = commandProcessor.blockingProcessCommand(command, lastLocalRevisions[it.noteId])
                        lastLocalRevisions[it.noteId] = event?.revision
                    } catch (t: Throwable) {
                        noteIdsWithErrors += it.noteId
                        logger.debug("Command not processed locally: $command + ${lastLocalRevisions[it.noteId]}", t)
                    }
                }
    }

    fun getConflicts(): Observable<Set<String>> {
        return Observable.empty()
    }

}
