package info.maaskant.wmsnotes.server.command

import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.server.command.grpc.Command
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import info.maaskant.wmsnotes.utilities.logger
import io.grpc.stub.StreamObserver
import org.lognet.springboot.grpc.GRpcService

@GRpcService
class CommandService(private val commandProcessor: CommandProcessor, private val grpcCommandMapper: GrpcCommandMapper) : CommandServiceGrpc.CommandServiceImplBase() {

    private val logger by logger()

    override fun postCommand(
            request: Command.PostCommandRequest,
            responseObserver: StreamObserver<Command.PostCommandResponse>
    ) {
        try {
            val command = grpcCommandMapper.toModelCommand(request)
            val event = commandProcessor.blockingProcessCommand(command)
            val response = Command.PostCommandResponse.newBuilder()
                    .setStatus(when (event) {
                        null -> Command.PostCommandResponse.Status.INTERNAL_ERROR // TODO
                        else -> Command.PostCommandResponse.Status.SUCCESS
                    })
                    .setNewEventId(event?.eventId ?: 0)
                    .setNoteId(event?.noteId ?: "")
                    .setNewRevision(event?.revision ?: 0)
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