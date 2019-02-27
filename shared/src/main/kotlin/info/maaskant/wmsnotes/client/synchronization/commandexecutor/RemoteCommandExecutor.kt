package info.maaskant.wmsnotes.client.synchronization.commandexecutor

import info.maaskant.wmsnotes.client.api.GrpcCommandMapper
import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse
import info.maaskant.wmsnotes.utilities.logger
import io.grpc.Deadline
import io.grpc.Status
import io.grpc.StatusRuntimeException
import javax.inject.Inject

class RemoteCommandExecutor @Inject constructor(
        private val grpcCommandMapper: GrpcCommandMapper,
        private val grpcCommandService: CommandServiceGrpc.CommandServiceBlockingStub,
        private val grpcDeadline: Deadline?
) : CommandExecutor {
    private val logger by logger()

    override fun execute(command: Command): CommandExecutor.ExecutionResult =
            try {
                command
                        .let {
                            logger.debug("Executing command remotely: $command")
                            grpcCommandMapper.toGrpcPostCommandRequest(it)
                        }
                        .let {
                            grpcCommandService
                                    .apply { if (grpcDeadline != null) this.withDeadline(grpcDeadline) }
                                    .postCommand(it)
                        }
                        .let { response ->
                            if (response.status == PostCommandResponse.Status.SUCCESS) {
                                val result = if (response.newEventId != 0) {
                                    if (response.noteId.isNotEmpty() && response.newRevision != 0) {
                                        CommandExecutor.ExecutionResult.Success(newEventMetadata = CommandExecutor.EventMetadata(
                                                eventId = response.newEventId,
                                                noteId = response.noteId,
                                                revision = response.newRevision
                                        ))
                                    } else {
                                        logger.warn("Executing command remotely returned success, but response was incomplete: $command -> (${response.status}, ${response.newEventId}, ${response.noteId}, ${response.newRevision})")
                                        CommandExecutor.ExecutionResult.Failure
                                    }
                                } else {
                                    CommandExecutor.ExecutionResult.Success(newEventMetadata = null)
                                }
                                logger.debug("Command successfully executed remotely: $command")
                                result
                            } else {
                                logger.debug("Executing $command remotely failed: ${response.status} ${response.errorDescription}")
                                CommandExecutor.ExecutionResult.Failure
                            }
                        }
            } catch (e: StatusRuntimeException) {
                when (e.status.code) {
                    Status.Code.UNAVAILABLE -> {
                        logger.debug("Could not send command $command to server: server not available")
                    }
                    Status.Code.DEADLINE_EXCEEDED -> {
                        logger.debug("Could not send command $command to server: server is taking too long to respond")
                    }
                    else -> logger.warn("Error sending command $command to server: ${e.status.code}, ${e.status.description}")
                }
                CommandExecutor.ExecutionResult.Failure
            } catch (t: Throwable) {
                logger.debug("Executing command $command remotely failed due to a local error", t)
                CommandExecutor.ExecutionResult.Failure
            }
}
