package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.utilities.logger
import io.grpc.StatusRuntimeException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalEventImporter @Inject constructor(
        private val eventStore: EventStore,
        private val eventRepository: ModifiableEventRepository,
        private val state: ImporterStateStorage
) {

    private val logger by logger()

    fun loadAndStoreLocalEvents() {
        logger.debug("Retrieving new local events")
        var numberOfNewEvents = 0
        try {
            eventStore.getEvents(state.lastEventId).blockingSubscribe {
                logger.debug("Storing new local event: $it")
                eventRepository.addEvent(it)
                state.lastEventId = it.eventId
                numberOfNewEvents++
            }
        } catch (e: StatusRuntimeException) {
            logger.warn("Error while retrieving events: ${e.status.code}")
            return
        } finally {
            if (numberOfNewEvents > 0) logger.info("Added $numberOfNewEvents new local events")
        }

    }

}