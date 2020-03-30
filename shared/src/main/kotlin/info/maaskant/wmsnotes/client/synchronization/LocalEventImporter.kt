package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.utilities.logger
import info.maaskant.wmsnotes.utilities.persistence.StateProducer
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalEventImporter @Inject constructor(
        private val eventStore: EventStore,
        private val eventRepository: ModifiableEventRepository,
        initialState: EventImporterState?
) : StateProducer<EventImporterState> {

    private val logger by logger()
    private var state = initialState ?: EventImporterState(null)
    private val stateUpdates: Subject<EventImporterState> = BehaviorSubject.create<EventImporterState>().toSerialized()

    fun loadAndStoreLocalEvents() {
        logger.debug("Retrieving new local events")
        var numberOfNewEvents = 0
        try {
            eventStore.getEvents(state.lastEventId).blockingSubscribe({
                logger.debug("Storing new local event: {}", it)
                eventRepository.addEvent(it)
                updateLastEventId(it.eventId)
                numberOfNewEvents++
            }, { logger.error("Error", it) })
        } finally {
            if (numberOfNewEvents > 0) logger.info("Added {} new local events", numberOfNewEvents)
        }

    }

    private fun updateLastEventId(lastEventId: Int) {
        state = state.copy(lastEventId = lastEventId)
        stateUpdates.onNext(state)
    }

    override fun getStateUpdates(): Observable<EventImporterState> = stateUpdates
}