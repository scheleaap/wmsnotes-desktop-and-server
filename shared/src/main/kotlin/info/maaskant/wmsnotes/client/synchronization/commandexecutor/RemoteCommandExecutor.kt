package info.maaskant.wmsnotes.client.synchronization.commandexecutor

import arrow.core.*
import arrow.core.Either.Companion.left
import arrow.core.Either.Companion.right
import info.maaskant.wmsnotes.client.api.GrpcCommandMapper
import info.maaskant.wmsnotes.client.synchronization.commandexecutor.CommandExecutor.*
import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.CommandError
import info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandRequest
import info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import info.maaskant.wmsnotes.utilities.logger
import io.grpc.Deadline
import io.grpc.Status
import io.grpc.StatusRuntimeException
import javax.inject.Inject

class RemoteCommandExecutor @Inject constructor(
        private val grpcCommandMapper: GrpcCommandMapper,
        grpcCommandService: CommandServiceGrpc.CommandServiceBlockingStub,
        grpcDeadline: Deadline?
) : CommandExecutor {
    private val logger by logger()

    private val grpcCommandService: CommandServiceGrpc.CommandServiceBlockingStub =
            grpcCommandService.apply { if (grpcDeadline != null) this.withDeadline(grpcDeadline) }

    override fun execute(command: Command, lastRevision: Int): ExecutionResult {
        val executionResult: ExecutionResult = try {
            logger.debug("Executing command remotely: $command, $lastRevision")
            val request = grpcCommandMapper.toGrpcPostCommandRequest(command, lastRevision)
            val responseEither: Either<StatusRuntimeException, PostCommandResponse> = postCommand(request)
            val a = responseEither.left().flatMap { left(1) }
            val b: ExecutionResult = responseEither.fold({ sre ->
                // Failure
                val commandError: CommandError = when (sre.status.code) {
                    Status.Code.INVALID_ARGUMENT ->
                        CommandError.InvalidCommandError(sre.status.description ?: "Missing description")
                    Status.Code.FAILED_PRECONDITION ->
                        CommandError.IllegalStateError(sre.status.description ?: "Missing description")
                    Status.Code.UNAVAILABLE ->
                        CommandError.NetworkError("Server not available")
                    Status.Code.DEADLINE_EXCEEDED ->
                        CommandError.NetworkError("Server is taking too long to respond")
                    else -> CommandError.OtherError(
                            message = "Error sending command to server: ${sre.status.code}, ${sre.status.description}",
                            cause = Some(sre)
                    )
                }
                ExecutionResult.Failure(commandError)
            }, { response ->
                // Success
                val result: ExecutionResult = if (response.newEventId != 0) {
                    if (response.aggregateId.isNotEmpty() && response.newRevision != 0) {
                        ExecutionResult.Success(EventMetadata(
                                eventId = response.newEventId,
                                aggId = response.aggregateId,
                                revision = response.newRevision
                        ))
                    } else {
                        val message = "Server returned success, but response was incomplete: " +
                                "${response.newEventId}, ${response.aggregateId}, ${response.newRevision}"
                        ExecutionResult.Failure(CommandError.OtherError(message))
                    }
                } else {
                    ExecutionResult.Success(newEventMetadata = null)
                }
                result
            })
            TODO()
        } catch (t: Throwable) {
            ExecutionResult.Failure(CommandError.OtherError("Unexpected error", cause = Some(t.nonFatalOrThrow())))
        }
        when (executionResult) {
            TODO HIER BEZIG: AFHANKELIJK VAN TYPE FOUT OP DEBUG OF WARNING LOGGEN
            is ExecutionResult.Failure -> logger.debug("Executing command remotely failed: $command, $lastRevision, ${executionResult.error}")
            is ExecutionResult.Success -> logger.debug("Command executed remotely successfully: $command")
        }
        return executionResult
    }

    private fun postCommand(request: PostCommandRequest): Either<StatusRuntimeException, PostCommandResponse> =
            try {
                right(grpcCommandService.postCommand(request))
            } catch (e: StatusRuntimeException) {
                left(e)
            }
}
