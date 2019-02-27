package info.maaskant.wmsnotes.desktop.client.indexing

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import info.maaskant.wmsnotes.model.NoteUndeletedEvent
import info.maaskant.wmsnotes.model.eventstore.EventStore
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// Future tests to add:
// - TODO Event: Change title
// - TODO Event: Move
internal class NoteIndexTest {
    private val noteId = "note-1"
    private val title = "Title 1"
    private val noteCreatedEvent = NoteCreatedEvent(eventId = 0, noteId = noteId, revision = 1, path = TODO(), title = title, content = TODO())

    private val scheduler = Schedulers.trampoline()

    private val eventStore: EventStore = mockk()

    private lateinit var noteIndexState: NoteIndexState

    private lateinit var eventUpdatesSubject: PublishSubject<Event>

    @BeforeEach
    fun init() {
        eventUpdatesSubject = PublishSubject.create<Event>()
        noteIndexState = NoteIndexState(isInitialized = false)
        clearMocks(
                eventStore
        )
        every { eventStore.getEvents() }.returns(Observable.empty())
        every { eventStore.getEventUpdates() }.returns(eventUpdatesSubject as Observable<Event>)
    }

    @Test
    fun `note created`() {
        // Given
        val index = NoteIndex(eventStore, noteIndexState, scheduler)

        // When
        eventUpdatesSubject.onNext(noteCreatedEvent)
        val observer = index.getNotes().test()

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values().toSet()).isEqualTo(setOf(NoteMetadata(noteId, title)))
    }

    @Test
    fun `note deleted`() {
        // Given
        val index = NoteIndex(eventStore, noteIndexState, scheduler)
        eventUpdatesSubject.onNext(noteCreatedEvent)

        // When
        eventUpdatesSubject.onNext(NoteDeletedEvent(eventId = 0, noteId = noteId, revision = 1))
        val observer = index.getNotes().test()

        // Then
        observer.await()
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values().toSet()).isEqualTo(emptySet<NoteMetadata>())
    }

    @Test
    fun `note undeleted`() {
        // Given
        val index = NoteIndex(eventStore, noteIndexState, scheduler)
        eventUpdatesSubject.onNext(noteCreatedEvent)
        eventUpdatesSubject.onNext(NoteDeletedEvent(eventId = 0, noteId = noteId, revision = 1))

        // When
        eventUpdatesSubject.onNext(NoteUndeletedEvent(eventId = 0, noteId = noteId, revision = 1))
        val observer = index.getNotes().test()

        // Then
        observer.await()
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values().toSet()).isEqualTo(setOf(NoteMetadata(noteId, title)))
    }

    @Test
    fun initialize() {
        // Given
        every { eventStore.getEvents() }.returns(Observable.just(noteCreatedEvent))
        val index1 = NoteIndex(eventStore, noteIndexState, scheduler) // Instantiate twice to test double initialization
        val stateObserver = index1.getStateUpdates().test()
        val index2 = NoteIndex(eventStore, stateObserver.values().last(), scheduler)

        // When
        val notesObserver = index2.getNotes().test()

        // Then
        notesObserver.assertComplete()
        notesObserver.assertNoErrors()
        assertThat(notesObserver.values().toSet()).isEqualTo(setOf(NoteMetadata(noteId, title)))
        verify(exactly = 1) {
            eventStore.getEvents(any())
        }
    }

    @Test
    fun `read state`() {
        // Given
        val index1 = NoteIndex(eventStore, noteIndexState, scheduler) // This instance is supposed to save the state
        val stateObserver = index1.getStateUpdates().test()
        eventUpdatesSubject.onNext(noteCreatedEvent)
        val index2 = NoteIndex(eventStore, stateObserver.values().last(), scheduler) // This instance is supposed to read the state

        // When
        val notesObserver = index2.getNotes().test()

        // Then
        notesObserver.assertComplete()
        notesObserver.assertNoErrors()
        assertThat(notesObserver.values().toSet()).isEqualTo(setOf(NoteMetadata(noteId, title)))
    }
}