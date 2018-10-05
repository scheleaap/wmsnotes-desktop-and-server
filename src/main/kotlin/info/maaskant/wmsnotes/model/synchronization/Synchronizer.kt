package info.maaskant.wmsnotes.model.synchronization

import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.server.api.GrpcConverters
import info.maaskant.wmsnotes.server.command.grpc.Command
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
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
        val noteIdsWithErrors: MutableSet<String> = HashSet<String>()

        localEvents
                .getCurrentEvents()
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

        remoteEvents.getCurrentEvents()
                .forEach {
                    logger.debug("Processing remote event: $it")
                    val command = remoteEventToLocalCommandMapper.map(it)
                    val event = commandProcessor.blockingProcessCommand(command, lastLocalRevisions[it.noteId])
                    lastLocalRevisions[it.noteId] = event?.revision
                }
    }

}
