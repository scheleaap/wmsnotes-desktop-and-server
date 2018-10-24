package info.maaskant.wmsnotes.client.indexing

import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.eventstore.EventStore
import io.mockk.*
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mapdb.DB
import org.mapdb.DBMaker

// Future tests to add:
// - Event: Change title
// - Event: Move
internal class NoteIndexTest {

    private val noteId = "note-1"

    private val scheduler = Schedulers.trampoline()

    private val eventStore: EventStore = mockk()

    private lateinit var database: DB

    private lateinit var eventUpdatesSubject: PublishSubject<Event>

    @BeforeEach
    fun init() {
        eventUpdatesSubject = PublishSubject.create<Event>()
        database = DBMaker.heapDB().make()
        clearMocks(
                eventStore
        )
        every { eventStore.getEvents() }.returns(Observable.empty())
        every { eventStore.getEventUpdates() }.returns(eventUpdatesSubject as Observable<Event>)
    }

    @Test
    fun `note created`() {
        // Given
        val index = NoteIndex(eventStore, database, scheduler)

        // When
        eventUpdatesSubject.onNext(NoteCreatedEvent(eventId = 0, noteId = noteId, revision = 1, title = "Title 1"))
        val observer = index.getNotes().test()

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values().toSet()).isEqualTo(setOf(NoteMetadata(noteId, "Title 1")))
    }

    @Test
    fun `note deleted`() {
        // Given
        val index = NoteIndex(eventStore, database, scheduler)
        eventUpdatesSubject.onNext(NoteCreatedEvent(eventId = 0, noteId = noteId, revision = 1, title = "Title 1"))

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
    fun persistence() {
        // Given
        NoteIndex(eventStore, database, scheduler) // This instance is supposed to save the index
        eventUpdatesSubject.onNext(NoteCreatedEvent(eventId = 0, noteId = noteId, revision = 1, title = "Title 1"))
        val index = NoteIndex(eventStore, database, scheduler) // This instance is supposed to read the index from disk

        // When
        val observer = index.getNotes().test()

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values().toSet()).isEqualTo(setOf(NoteMetadata(noteId, "Title 1")))
    }

    @Test
    fun initialize() {
        // Given
        every { eventStore.getEvents() }.returns(Observable.just(NoteCreatedEvent(eventId = 0, noteId = noteId, revision = 1, title = "Title 1")))
        NoteIndex(eventStore, database, scheduler) // Instantiate twice to test double initialization
        val index = NoteIndex(eventStore, database, scheduler)

        // When
        val observer = index.getNotes().test()

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values().toSet()).isEqualTo(setOf(NoteMetadata(noteId, "Title 1")))
        verify(exactly = 1) {
            eventStore.getEvents(any())
        }
    }
}