package info.maaskant.wmsnotes.server.event

import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.server.command.grpc.Event
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import info.maaskant.wmsnotes.utilities.logger
import io.grpc.stub.StreamObserver
import org.lognet.springboot.grpc.GRpcService

@GRpcService
class EventService(private val eventStore: EventStore, private val grpcEventMapper: GrpcEventMapper) : EventServiceGrpc.EventServiceImplBase() {
    private val logger by logger()

    override fun getEvents(
            request: Event.GetEventsRequest,
            responseObserver: StreamObserver<Event.GetEventsResponse>
    ) {
        eventStore.getEvents(afterEventId = request.afterEventId).subscribe(
                { responseObserver.onNext(grpcEventMapper.toGrpcGetEventsResponse(it)) },
                {
                    logger.warn("Internal error", it)
                    responseObserver.onError(it)
                },
                { responseObserver.onCompleted() }
        )

    }
}