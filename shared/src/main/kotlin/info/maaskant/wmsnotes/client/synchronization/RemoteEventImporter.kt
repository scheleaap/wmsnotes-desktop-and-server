package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.api.GrpcEventMapper
import info.maaskant.wmsnotes.client.api.UnknownEventTypeException
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.server.command.grpc.Event
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import info.maaskant.wmsnotes.utilities.logger
import info.maaskant.wmsnotes.utilities.persistence.StateProducer
import io.grpc.Deadline
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteEventImporter @Inject constructor(
        private val eventService: EventServiceGrpc.EventServiceBlockingStub,
        private val grpcDeadline: Deadline?,
        private val eventRepository: ModifiableEventRepository,
        private val grpcEventMapper: GrpcEventMapper,
        initialState: EventImporterState?
) : StateProducer<EventImporterState> {

    private val logger by logger()
    private var state = initialState ?: EventImporterState(null)
    private val stateUpdates: Subject<EventImporterState> = BehaviorSubject.create<EventImporterState>().toSerialized()

    fun loadAndStoreRemoteEvents() {
        logger.debug("Retrieving new remote events")
        var numberOfNewEvents = 0
        try {
            val request = createGetEventsRequest()
            val response: Iterator<Event.GetEventsResponse> = eventService
                    .apply { if (grpcDeadline != null) withDeadline(grpcDeadline) }
                    .getEvents(request)
            response.forEach {
                val event = grpcEventMapper.toModelClass(it)
                logger.debug("Storing new remote event: {}", event)
                eventRepository.addEvent(event)
                updateLastEventId(event.eventId)
                numberOfNewEvents++
            }
        } catch (e: StatusRuntimeException) {
            when (e.status.code) {
                Status.Code.UNAVAILABLE -> logger.debug("Could not retrieve events: server not available")
                Status.Code.DEADLINE_EXCEEDED -> logger.debug("Could not retrieve events: server is taking too long to respond")
                else -> logger.warn("Error while retrieving events: {}, {}", e.status.code, e.status.description)
            }
        } catch (e: UnknownEventTypeException) {
            logger.warn("The server retrieved one or more unknown event types ($e). No further events will be imported.")
        } finally {
            if (numberOfNewEvents > 0) logger.info("Added {} new remote events", numberOfNewEvents)
        }
    }

    private fun createGetEventsRequest(): Event.GetEventsRequest {
        return Event.GetEventsRequest.newBuilder()
                .setAfterEventId(state.lastEventId ?: 0)
                .build()
    }

    private fun updateLastEventId(lastEventId: Int) {
        state = state.copy(lastEventId = lastEventId)
        stateUpdates.onNext(state)
    }

    override fun getStateUpdates(): Observable<EventImporterState> = stateUpdates
}
