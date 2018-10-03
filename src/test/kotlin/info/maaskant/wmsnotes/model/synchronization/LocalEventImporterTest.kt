package info.maaskant.wmsnotes.model.synchronization

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.EventStore
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.eventrepository.ModifiableEventRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import io.reactivex.Completable
import io.reactivex.Observable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class LocalEventImporterTest {

    private val eventStore: EventStore = mockk()
    private val eventRepository: ModifiableEventRepository = mockk()

    @BeforeEach
    fun init() {
        clearMocks(eventStore, eventRepository)
        every { eventRepository.addEvent(any()) }.returns(Completable.complete())
    }

    @Test
    fun `store new events`() {
        // Given
        val event1 = modelNoteEvent(id = 1)
        val event2 = modelNoteEvent(id = 2)
        every { eventStore.getEvents(any()) }.returns(Observable.just(event1, event2))
        val importer = LocalEventImporter(eventStore, eventRepository, InMemoryStateProperty())

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
        val event1 = modelNoteEvent(id = 1)
        val event2 = modelNoteEvent(id = 2)
        every { eventStore.getEvents(afterEventId = null) }.returns(Observable.just(event1))
        every { eventStore.getEvents(afterEventId = 1) }.returns(Observable.empty<Event>())
        val stateProperty = InMemoryStateProperty()

        // When
        val importer1 = LocalEventImporter(eventStore, eventRepository, stateProperty)
        importer1.loadAndStoreLocalEvents()

        // Given
        every { eventStore.getEvents(afterEventId = null) }.returns(Observable.just(event1, event2))
        every { eventStore.getEvents(afterEventId = 1) }.returns(Observable.just(event2))

        // When
        val importer2 = LocalEventImporter(eventStore, eventRepository, stateProperty)
        importer2.loadAndStoreLocalEvents()

        // Then
        verifySequence {
            eventStore.getEvents(afterEventId = null)
            eventRepository.addEvent(event1)
            eventStore.getEvents(afterEventId = 1)
            eventRepository.addEvent(event2)
        }
    }

}

private fun modelNoteEvent(id: Int): NoteCreatedEvent {
    return NoteCreatedEvent(id, "note-$id", "Title $id")
}
