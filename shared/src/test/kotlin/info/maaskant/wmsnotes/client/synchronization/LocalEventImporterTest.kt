package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.model.Path
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import io.reactivex.Observable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class LocalEventImporterTest {

    private val eventStore: EventStore = mockk()
    private val eventRepository: ModifiableEventRepository = mockk()

    @BeforeEach
    fun init() {
        clearMocks(eventStore, eventRepository)
        every { eventRepository.addEvent(any()) }.answers {}
    }

    @Test
    fun `load and store events`() {
        // Given
        val event1 = modelEvent(i = 1)
        val event2 = modelEvent(i = 2)
        every { eventStore.getEvents(any()) }.returns(Observable.just(event1, event2))
        val importer = LocalEventImporter(eventStore, eventRepository, EventImporterState(null))

        // When
        importer.loadAndStoreLocalEvents()

        // Then
        verifySequence {
            eventStore.getEvents(afterEventId = null)
            eventRepository.addEvent(event1)
            eventRepository.addEvent(event2)
        }
    }

    @Test
    fun `only load new events`() {
        // Given
        val event1 = modelEvent(i = 1)
        val event2 = modelEvent(i = 2)
        every { eventStore.getEvents(afterEventId = null) }.returns(Observable.just(event1))
        every { eventStore.getEvents(afterEventId = 1) }.returns(Observable.empty<Event>())

        // When
        val importer1 = LocalEventImporter(eventStore, eventRepository, EventImporterState(null))
        val stateObserver = importer1.getStateUpdates().test()
        importer1.loadAndStoreLocalEvents()

        // Given
        every { eventStore.getEvents(afterEventId = null) }.returns(Observable.just(event1, event2))
        every { eventStore.getEvents(afterEventId = 1) }.returns(Observable.just(event2))

        // When
        val importer2 = LocalEventImporter(eventStore, eventRepository, stateObserver.values().last())
        importer2.loadAndStoreLocalEvents()

        // Then
        verifySequence {
            eventStore.getEvents(afterEventId = null)
            eventRepository.addEvent(event1)
            eventStore.getEvents(afterEventId = 1)
            eventRepository.addEvent(event2)
        }
    }

    companion object {
        internal fun modelEvent(i: Int): NoteCreatedEvent {
            return NoteCreatedEvent(eventId = i, aggId = "note-$i", revision = i, path = Path("path-$i"), title = "Title $i", content = "Text $i")
        }
    }
}
