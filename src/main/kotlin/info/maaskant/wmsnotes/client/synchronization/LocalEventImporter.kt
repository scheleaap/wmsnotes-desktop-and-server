package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
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
        logger.info("Getting and processing local events")
        try {
            eventStore.getEvents(state.lastEventId).blockingSubscribe {
                logger.debug("Storing new local event: $it")
                eventRepository.addEvent(it)
                state.lastEventId = it.eventId
            }
        } catch (e: StatusRuntimeException) {
            logger.warn("Error while retrieving events: ${e.status.code}")
            return
        }
    }

}