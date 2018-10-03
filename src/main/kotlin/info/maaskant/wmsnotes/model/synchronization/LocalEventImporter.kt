package info.maaskant.wmsnotes.model.synchronization

import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.model.EventStore
import info.maaskant.wmsnotes.model.eventrepository.ModifiableEventRepository
import io.grpc.StatusRuntimeException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalEventImporter @Inject constructor(
        private val eventStore: EventStore,
        private val eventRepository: ModifiableEventRepository,
        private val stateProperty: StateProperty
) {

    private val logger by logger()

    fun loadAndStoreLocalEvents() {
        logger.info("Getting and processing local events")
        try {
            eventStore.getCurrentEvents(stateProperty.get()).blockingSubscribe {
                logger.debug("Storing new local event: $it")
                eventRepository.addEvent(it)
                stateProperty.put(it.eventId)
            }
        } catch (e: StatusRuntimeException) {
            logger.warn("Error while retrieving events: ${e.status.code}")
            return
        }
    }

}