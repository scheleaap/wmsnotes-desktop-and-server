package info.maaskant.wmsnotes.server.command

import info.maaskant.wmsnotes.model.CommandBus
import info.maaskant.wmsnotes.model.CommandExecution
import info.maaskant.wmsnotes.server.command.grpc.Command
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import info.maaskant.wmsnotes.utilities.logger
import io.grpc.stub.StreamObserver
import org.lognet.springboot.grpc.GRpcService
import java.util.concurrent.TimeUnit

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
            logger.debug("Received request: {}", commandRequest)
            val commandResult = CommandExecution.executeBlocking(commandBus = commandBus, commandRequest = commandRequest, timeout = CommandExecution.Duration(120, TimeUnit.SECONDS))
            val response: Command.PostCommandResponse = if (commandResult.newEvents.size > 1) {
                throw IllegalStateException("$commandRequest produced ${commandResult.newEvents.size} events")
            } else {
                val newEvent = commandResult.newEvents.firstOrNull()
                Command.PostCommandResponse.newBuilder()
                        .setStatus(when (commandResult.allSuccessful) {
                            true -> Command.PostCommandResponse.Status.SUCCESS
                            else -> Command.PostCommandResponse.Status.INTERNAL_ERROR
                        })
                        .setNewEventId(newEvent?.eventId ?: 0)
                        .setAggregateId(newEvent?.aggId ?: "")
                        .setNewRevision(newEvent?.revision ?: 0)
                        .build()
            }
            responseObserver.onNext(response)
        } catch (e: Throwable) {
            responseObserver.onNext(toErrorResponse(e))
        }
        responseObserver.onCompleted()
    }

    private fun toErrorResponse(e: Throwable): Command.PostCommandResponse {
        val responseBuilder = Command.PostCommandResponse.newBuilder()
        if (e is BadRequestException) {
            logger.info("Bad request: {}", e.message)
            responseBuilder
                    .setStatus(Command.PostCommandResponse.Status.BAD_REQUEST)
                    .setErrorDescription(e.message)
        } else {
            logger.warn("Internal error", e)
            responseBuilder
                    .setStatus(Command.PostCommandResponse.Status.INTERNAL_ERROR)
                    .setErrorDescription("Internal errror")
        }
        return responseBuilder.build()
    }

}