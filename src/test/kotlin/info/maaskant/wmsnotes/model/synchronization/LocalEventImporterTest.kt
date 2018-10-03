package info.maaskant.wmsnotes.model.synchronization

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.EventStore
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.eventrepository.ModifiableEventRepository
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
    fun `store new events`() {
        // Given
        val event1 = modelEvent(id = 1)
        val event2 = modelEvent(id = 2)
        every { eventStore.getCurrentEvents(any()) }.returns(Observable.just(event1, event2))
        val importer = LocalEventImporter(eventStore, eventRepository, InMemoryStateProperty())

        // When
        importer.loadAndStoreLocalEvents()

        // Then
        verifySequence {
            eventStore.getCurrentEvents(afterEventId = null)
            eventRepository.addEvent(event1)
            eventRepository.addEvent(event2)
        }
    }

    @Test
    fun `only load new events`() {
        // Given
        val event1 = modelEvent(id = 1)
        val event2 = modelEvent(id = 2)
        every { eventStore.getCurrentEvents(afterEventId = null) }.returns(Observable.just(event1))
        every { eventStore.getCurrentEvents(afterEventId = 1) }.returns(Observable.empty<Event>())
        val stateProperty = InMemoryStateProperty()

        // When
        val importer1 = LocalEventImporter(eventStore, eventRepository, stateProperty)
        importer1.loadAndStoreLocalEvents()

        // Given
        every { eventStore.getCurrentEvents(afterEventId = null) }.returns(Observable.just(event1, event2))
        every { eventStore.getCurrentEvents(afterEventId = 1) }.returns(Observable.just(event2))

        // When
        val importer2 = LocalEventImporter(eventStore, eventRepository, stateProperty)
        importer2.loadAndStoreLocalEvents()

        // Then
        verifySequence {
            eventStore.getCurrentEvents(afterEventId = null)
            eventRepository.addEvent(event1)
            eventStore.getCurrentEvents(afterEventId = 1)
            eventRepository.addEvent(event2)
        }
    }

}

private fun modelEvent(id: Int): NoteCreatedEvent {
    return NoteCreatedEvent(id, "note-$id", "Title $id")
}
