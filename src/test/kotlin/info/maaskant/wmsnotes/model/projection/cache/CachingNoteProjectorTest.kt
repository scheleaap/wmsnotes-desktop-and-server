package info.maaskant.wmsnotes.model.projection.cache

import info.maaskant.wmsnotes.model.AttachmentAddedEvent
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.projection.CachingNoteProjector
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.NoteProjector
import io.mockk.*
import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CachingNoteProjectorTest {
    private val noteId = "note"

    private val event1 = NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = "Title")
    private val event2 = AttachmentAddedEvent(eventId = 2, noteId = noteId, revision = 2, name = "att-1", content = "data1".toByteArray())
    private val event3 = AttachmentAddedEvent(eventId = 3, noteId = noteId, revision = 3, name = "att-2", content = "data2".toByteArray())

    private val noteAfterEvent1 = Note().apply(event1).component1()
    private val noteAfterEvent2 = noteAfterEvent1.apply(event2).component1()
    private val noteAfterEvent3 = noteAfterEvent2.apply(event3).component1()

    private val wrappedProjector: NoteProjector = mockk()
    private val noteCache: NoteCache = mockk()
    private val eventStore: EventStore = mockk()

    @BeforeEach
    fun init() {
        clearMocks(
                wrappedProjector,
                noteCache
        )
        every { noteCache.put(any()) } just Runs
    }

    @Test
    fun `project, not present in cache`() {
        // Given
        every { wrappedProjector.project(noteId, noteAfterEvent2.revision) }.returns(noteAfterEvent2)
        every { noteCache.get(noteId, noteAfterEvent1.revision) }.returns(null)
        every { noteCache.get(noteId, noteAfterEvent2.revision) }.returns(null)
        every { noteCache.getLatest(noteId, noteAfterEvent2.revision) }.returns(null)
        every { eventStore.getEventsOfNote(noteId, afterRevision = null) }.returns(Observable.just(event1, event2))
        val p = CachingNoteProjector(eventStore, noteCache)

        // When
        val note = p.project(noteId, noteAfterEvent2.revision)

        // Then
        assertThat(note).isEqualTo(noteAfterEvent2)
    }

    @Test
    fun `project, old version present in cache`() {
        // Given
        every { wrappedProjector.project(noteId, noteAfterEvent2.revision) }.throws(Exception())
        every { noteCache.get(noteId, noteAfterEvent1.revision) }.returns(noteAfterEvent1)
        every { noteCache.get(noteId, noteAfterEvent2.revision) }.returns(null)
        every { noteCache.getLatest(noteId, noteAfterEvent2.revision) }.returns(noteAfterEvent1)
        every { eventStore.getEventsOfNote(noteId, afterRevision = noteAfterEvent1.revision) }.returns(Observable.just(event2))
        val p = CachingNoteProjector(eventStore, noteCache)

        // When
        val note = p.project(noteId, noteAfterEvent2.revision)

        // Then
        assertThat(note).isEqualTo(noteAfterEvent2)
    }

    @Test
    fun `project, too new version present in cache`() {
        // Given
        every { wrappedProjector.project(noteId, noteAfterEvent2.revision) }.throws(Exception())
        every { noteCache.get(noteId, noteAfterEvent1.revision) }.returns(null)
        every { noteCache.get(noteId, noteAfterEvent2.revision) }.returns(null)
        every { noteCache.get(noteId, noteAfterEvent3.revision) }.returns(noteAfterEvent3)
        every { noteCache.getLatest(noteId, lastRevision = null) }.returns(noteAfterEvent3)
        every { noteCache.getLatest(noteId, lastRevision = noteAfterEvent2.revision) }.returns(noteAfterEvent2)
        every { eventStore.getEventsOfNote(noteId, afterRevision = noteAfterEvent2.revision) }.returns(Observable.empty())
        every { eventStore.getEventsOfNote(noteId, afterRevision = noteAfterEvent3.revision) }.returns(Observable.empty())
        val p = CachingNoteProjector(eventStore, noteCache)

        // When
        val note = p.project(noteId, noteAfterEvent2.revision)

        // Then
        assertThat(note).isEqualTo(noteAfterEvent2)
    }

    @Test
    fun `project with updates, nothing present in cache`() {
        // Given
        val events = createHotObservable()
        every { noteCache.getLatest(noteId, lastRevision = null) }.returns(null)
        every { eventStore.getEventsOfNote(noteId, afterRevision = null) }.returns(Observable.just(event1, event2))
        every { eventStore.getEventsOfNoteWithUpdates(noteId, afterRevision = noteAfterEvent2.revision) }.returns(events)
        val p = CachingNoteProjector(eventStore, noteCache)

        // When
        val observer = p.projectAndUpdate(noteId).test()
        events.onNext(event3)

        // Then
        observer.assertNotComplete()
        observer.assertNotTerminated()
        assertThat(observer.values()).isEqualTo(listOf(noteAfterEvent2, noteAfterEvent3))
        verify {
            noteCache.put(noteAfterEvent2)
            noteCache.put(noteAfterEvent3)
        }
    }

    @Test
    fun `project with updates, old version present in cache`() {
        // Given
        val events = createHotObservable()
        every { noteCache.getLatest(noteId, lastRevision = null) }.returns(noteAfterEvent1)
        every { eventStore.getEventsOfNote(noteId, afterRevision = noteAfterEvent1.revision) }.returns(Observable.just(event2))
        every { eventStore.getEventsOfNoteWithUpdates(noteId, afterRevision = noteAfterEvent2.revision) }.returns(events)
        val p = CachingNoteProjector(eventStore, noteCache)

        // When
        val observer = p.projectAndUpdate(noteId).test()
        events.onNext(event3)

        // Then
        observer.assertNotComplete()
        observer.assertNotTerminated()
        assertThat(observer.values()).isEqualTo(listOf(noteAfterEvent2, noteAfterEvent3))
    }

    @Test
    fun `project with updates, latest version present in cache`() {
        // Given
        val events = createHotObservable()
        every { noteCache.getLatest(noteId, lastRevision = null) }.returns(noteAfterEvent2)
        every { eventStore.getEventsOfNote(noteId, afterRevision = noteAfterEvent2.revision) }.returns(Observable.empty())
        every { eventStore.getEventsOfNoteWithUpdates(noteId, afterRevision = noteAfterEvent2.revision) }.returns(events)
        val p = CachingNoteProjector(eventStore, noteCache)

        // When
        val observer = p.projectAndUpdate(noteId).test()
        events.onNext(event3)

        // Then
        observer.assertNotComplete()
        observer.assertNotTerminated()
        assertThat(observer.values()).isEqualTo(listOf(noteAfterEvent2, noteAfterEvent3))
    }

    private fun createHotObservable(vararg events: Event): Subject<Event> {
        val subject = ReplaySubject.create<Event>()
        for (event in events) {
            subject.onNext(event)
        }
        return subject
    }

}