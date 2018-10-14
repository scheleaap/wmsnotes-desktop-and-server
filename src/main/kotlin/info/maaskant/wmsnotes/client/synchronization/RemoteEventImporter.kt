package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.server.api.GrpcEventMapper
import info.maaskant.wmsnotes.server.command.grpc.Event
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import io.grpc.StatusRuntimeException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteEventImporter @Inject constructor(
        private val eventService: EventServiceGrpc.EventServiceBlockingStub,
        private val eventRepository: ModifiableEventRepository,
        private val grpcEventMapper: GrpcEventMapper,
        private val state: ImporterStateStorage
) {

    private val logger by logger()

    fun loadAndStoreRemoteEvents() {
        logger.info("Getting and processing remote events")
        try {
            val request = createGetEventsRequest()
            val response: Iterator<Event.GetEventsResponse> = eventService.getEvents(request)
            response.forEachRemaining {
                val event = grpcEventMapper.toModelClass(it)
                logger.debug("Storing new remote event: $event")
                eventRepository.addEvent(event)
                state.lastEventId = event.eventId
            }
        } catch (e: StatusRuntimeException) {
            logger.warn("Error while retrieving events: ${e.status.code}")
            return
        }
    }

    private fun createGetEventsRequest(): Event.GetEventsRequest? {
        val builder = Event.GetEventsRequest.newBuilder()
        if (state.lastEventId != null) {
            builder.afterEventId = state.lastEventId!!
        }
        return builder.build()
    }

}