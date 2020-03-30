package info.maaskant.wmsnotes.server.command

import arrow.core.Either
import arrow.core.Either.*
import arrow.core.Option
import arrow.core.getOrElse
import info.maaskant.wmsnotes.model.CommandBus
import info.maaskant.wmsnotes.model.CommandError
import info.maaskant.wmsnotes.model.CommandError.*
import info.maaskant.wmsnotes.model.CommandExecution
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.server.command.grpc.Command
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import info.maaskant.wmsnotes.utilities.logger
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import org.lognet.springboot.grpc.GRpcService

@Suppress("MoveVariableDeclarationIntoWhen")
@GRpcService
class CommandService(
        private val grpcCommandMapper: GrpcCommandMapper,
        private val commandBus: CommandBus,
        private val commandExecutionTimeout: CommandExecution.Duration
) : CommandServiceGrpc.CommandServiceImplBase() {

    private val logger by logger()

    @Synchronized
    override fun postCommand(
            request: Command.PostCommandRequest,
            responseObserver: StreamObserver<Command.PostCommandResponse>
    ) {
        try {
            val commandRequest = grpcCommandMapper.toModelCommandRequest(request)
            logger.debug("Received request: {}", commandRequest)
            if (commandRequest.commands.size > 1) {
                val message = "Request contains more than one command: $commandRequest"
                logger.warn(message)
                responseObserver.onError(Status.INTERNAL
                        .withDescription(message)
                        .asRuntimeException()
                )
            } else {
                val commandResult = CommandExecution.executeBlocking(commandBus = commandBus, commandRequest = commandRequest, timeout = commandExecutionTimeout)
                if (commandResult.outcome.size != 1) {
                    val message = "Result does not contain 1 command but ${commandResult.outcome.size}: $commandRequest -> $commandResult"
                    logger.warn(message)
                    responseObserver.onError(Status.INTERNAL
                            .withDescription(message)
                            .asRuntimeException()
                    )
                } else {
                    val outcome: Either<CommandError, Option<Event>> = commandResult.outcome.first().second
                    when (outcome) {
                        is Left -> responseObserver.onError(commandErrorToStatusRuntimeException(outcome.a))
                        is Right -> {
                            val response = eventToResponse(outcome.b)
                            responseObserver.onNext(response)
                            responseObserver.onCompleted()
                        }
                    }
                }
            }
        } catch (t: InvalidRequestException) {
            // TODO Stop throwing exceptions
            logger.info("Bad request: {}", t.message)
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(t.message)
                    .withCause(t)
                    .asRuntimeException()
            )
        } catch (t: Throwable) {
            logger.warn("Internal error", t)
            responseObserver.onError(Status.INTERNAL
                    .withCause(t)
                    .asRuntimeException()
            )
        }
    }

    private fun commandErrorToStatusRuntimeException(commandError: CommandError): StatusRuntimeException? {
        return when (commandError) {
            is IllegalStateError -> Status.FAILED_PRECONDITION
                    .withDescription(commandError.message)
                    .asRuntimeException()
            is InvalidCommandError -> Status.INVALID_ARGUMENT
                    .withDescription(commandError.message)
                    .asRuntimeException()
            is NetworkError ->
                // Cannot happen, really.
                Status.INTERNAL
                        .withDescription(commandError.message)
                        .withCause(commandError.cause.orNull())
                        .asRuntimeException()
            is OtherError -> Status.INTERNAL
                    .withDescription(commandError.message)
                    .withCause(commandError.cause.orNull())
                    .asRuntimeException()
            is StorageError -> Status.INTERNAL
                    .withDescription(commandError.message)
                    .withCause(commandError.cause.orNull())
                    .asRuntimeException()
        }
    }

    private fun eventToResponse(eventOption: Option<Event>): Command.PostCommandResponse? {
        return eventOption.map { event ->
            Command.PostCommandResponse.newBuilder()
                    .setNewEventId(event.eventId)
                    .setAggregateId(event.aggId)
                    .setNewRevision(event.revision)
                    .build()
        }.getOrElse {
            Command.PostCommandResponse.newBuilder()
                    .setNewEventId(0)
                    .setAggregateId("")
                    .setNewRevision(0)
                    .build()
        }
    }
}