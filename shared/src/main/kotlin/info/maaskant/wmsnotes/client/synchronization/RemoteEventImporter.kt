package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.api.GrpcEventMapper
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.server.command.grpc.Event
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import info.maaskant.wmsnotes.utilities.logger
import info.maaskant.wmsnotes.utilities.persistence.StateProducer
import io.grpc.StatusRuntimeException
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteEventImporter @Inject constructor(
        private val eventService: EventServiceGrpc.EventServiceBlockingStub,
        private val eventRepository: ModifiableEventRepository,
        private val grpcEventMapper: GrpcEventMapper,
        initialState: EventImporterState?
) : StateProducer<EventImporterState> {

    private val logger by logger()
    private var state = initialState ?: EventImporterState(null)
    private val stateUpdates: BehaviorSubject<EventImporterState> = BehaviorSubject.create()

    fun loadAndStoreRemoteEvents() {
        logger.debug("Retrieving new remote events")
        var numberOfNewEvents = 0
        try {
            val request = createGetEventsRequest()
            val response: Iterator<Event.GetEventsResponse> = eventService.getEvents(request)
            response.forEachRemaining {
                val event = grpcEventMapper.toModelClass(it)
                logger.debug("Storing new remote event: $event")
                eventRepository.addEvent(event)
                updateLastEventId(it.eventId)
                numberOfNewEvents++
            }
        } catch (e: StatusRuntimeException) {
            logger.warn("Error while retrieving events: ${e.status.code}")
            return
        } finally {
            if (numberOfNewEvents > 0) logger.info("Added $numberOfNewEvents new remote events")
        }
    }

    private fun createGetEventsRequest(): Event.GetEventsRequest? {
        val builder = Event.GetEventsRequest.newBuilder()
        if (state.lastEventId != null) {
            builder.afterEventId = state.lastEventId!!
        }
        return builder.build()
    }

    private fun updateLastEventId(lastEventId: Int) {
        state = state.copy(lastEventId = lastEventId)
        stateUpdates.onNext(state)
    }

    override fun getStateUpdates(): Observable<EventImporterState> = stateUpdates
}