package info.maaskant.wmsnotes.server.command

import info.maaskant.wmsnotes.model.CommandBus
import info.maaskant.wmsnotes.model.CommandExecution
import info.maaskant.wmsnotes.server.command.grpc.Command
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import info.maaskant.wmsnotes.utilities.logger
import io.grpc.stub.StreamObserver
import org.lognet.springboot.grpc.GRpcService

@GRpcService
class CommandService(
        private val grpcCommandMapper: GrpcCommandMapper,
        private val commandBus: CommandBus,
        private val commandExecutionTimeout: CommandExecution.Duration
) : CommandServiceGrpc.CommandServiceImplBase() {

    private val logger by logger()

    override fun postCommand(
            request: Command.PostCommandRequest,
            responseObserver: StreamObserver<Command.PostCommandResponse>
    ) {
        try {
            val commandRequest = grpcCommandMapper.toModelCommandRequest(request)
            val commandResult = CommandExecution.executeBlocking(commandBus = commandBus, commandRequest = commandRequest, timeout = commandExecutionTimeout)
            val newEvent = if (commandResult.newEvents.size > 1) {
                throw IllegalStateException("$commandRequest produced ${commandResult.newEvents.size} events")
            } else {
                commandResult.newEvents.firstOrNull()
            }
            val response = Command.PostCommandResponse.newBuilder()
                    .setStatus(when (newEvent) {
                        // TODO Distinguish between success and no event and error and no event
                        null -> Command.PostCommandResponse.Status.INTERNAL_ERROR
                        else -> Command.PostCommandResponse.Status.SUCCESS
                    })
                    .setNewEventId(newEvent?.eventId ?: 0)
                    .setAggregateId(newEvent?.aggId ?: "")
                    .setNewRevision(newEvent?.revision ?: 0)
                    .build()
            responseObserver.onNext(response)
        } catch (e: Throwable) {
            responseObserver.onNext(toErrorResponse(e))
        }
        responseObserver.onCompleted()
    }

    private fun toErrorResponse(e: Throwable): Command.PostCommandResponse {
        val responseBuilder = Command.PostCommandResponse.newBuilder()
        if (e is BadRequestException) {
            logger.info("Bad request: ${e.message}")
            responseBuilder.setStatus(Command.PostCommandResponse.Status.BAD_REQUEST).setErrorDescription(e.message)
        } else {
            logger.warn("Internal error", e)
            responseBuilder.setStatus(Command.PostCommandResponse.Status.INTERNAL_ERROR).setErrorDescription("Internal errror")
        }
        return responseBuilder.build()
    }

}