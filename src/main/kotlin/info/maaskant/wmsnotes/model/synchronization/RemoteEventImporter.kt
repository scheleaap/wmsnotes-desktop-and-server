package info.maaskant.wmsnotes.model.synchronization

import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.model.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.server.api.GrpcConverters
import info.maaskant.wmsnotes.server.command.grpc.Event
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import io.grpc.StatusRuntimeException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteEventImporter @Inject constructor(
        private val eventService: EventServiceGrpc.EventServiceBlockingStub,
        private val eventRepository: ModifiableEventRepository,
        private val stateProperty: StateProperty
) {

    private val logger by logger()

    fun loadAndStoreRemoteEvents() {
        logger.info("Getting and processing remote events")
        try {
            val request = createGetEventsRequest()
            val response: Iterator<Event.GetEventsResponse> = eventService.getEvents(request)
            response.forEachRemaining {
                val event = GrpcConverters.toModelClass(it)
                logger.debug("Storing new remote event: $event")
                eventRepository.addEvent(event)
                stateProperty.put(event.eventId)
            }
        } catch (e: StatusRuntimeException) {
            logger.warn("Error while retrieving events: ${e.status.code}")
            return
        }
    }

    private fun createGetEventsRequest(): Event.GetEventsRequest? {
        val builder = Event.GetEventsRequest.newBuilder()
        if (stateProperty.get() != null) {
            builder.afterEventId = stateProperty.get()!!
        }
        return builder.build()
    }

}