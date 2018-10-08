package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import io.reactivex.observers.TestObserver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal abstract class EventStoreTest {

    @Test
    fun `appendEvent, non-zero event id`() {
        // Given
        val event = NoteCreatedEvent(eventId = 1, noteId = "note", revision = 1, title = "Title")
        val r = createInstance()

        // When / Then
        assertThrows<IllegalArgumentException> {
            r.appendEvent(event)
        }
    }

    @Test
    fun `appendEvent and getEvents`() {
        // Given
        val eventsIn = listOf(
                NoteCreatedEvent(eventId = 0, noteId = "note-3", revision = 1, title = "Title 3"),
                NoteCreatedEvent(eventId = 0, noteId = "note-2", revision = 1, title = "Title 2"),
                NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title 1"),
                NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 2, title = "Title 1")
        )
        val eventsOut = listOf(
                NoteCreatedEvent(eventId = 2, noteId = eventsIn[1].noteId, revision = eventsIn[1].revision, title = eventsIn[1].title),
                NoteCreatedEvent(eventId = 3, noteId = eventsIn[2].noteId, revision = eventsIn[2].revision, title = eventsIn[2].title),
                NoteCreatedEvent(eventId = 4, noteId = eventsIn[3].noteId, revision = eventsIn[3].revision, title = eventsIn[3].title)
        )
        var r: EventStore = createInstance()
        eventsIn.forEach {
            r.appendEvent(it)
            r = createInstance()
        }
        val observer = TestObserver<Event>()

        // When
        r.getEvents(afterEventId = 1).subscribe(observer)

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(eventsOut)
    }

    @Test
    fun `appendEvent and getEventsOfNote`() {
        // Given
        val eventsIn = listOf(
                NoteCreatedEvent(eventId = 0, noteId = "note-3", revision = 1, title = "Title 3"),
                NoteCreatedEvent(eventId = 0, noteId = "note-2", revision = 1, title = "Title 2"),
                NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title 1"),
                NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 2, title = "Title 1")
        )
        val eventsOut = listOf(
                NoteCreatedEvent(eventId = 3, noteId = eventsIn[2].noteId, revision = eventsIn[2].revision, title = eventsIn[2].title),
                NoteCreatedEvent(eventId = 4, noteId = eventsIn[3].noteId, revision = eventsIn[3].revision, title = eventsIn[3].title)
        )
        var r: EventStore = createInstance()
        eventsIn.forEach {
            r.appendEvent(it)
            r = createInstance()
        }
        val observer = TestObserver<Event>()

        // When
        r.getEventsOfNote("note-1").subscribe(observer)

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(eventsOut)
    }

    @Test
    fun `appendEvent, revision already exists`() {
        // Given
        val event1 = NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title")
        val event2 = NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title")
        var r = createInstance()
        r.appendEvent(event1)
        r = createInstance()

        // When / Then
        assertThrows<IllegalArgumentException> {
            r.appendEvent(event2)
        }
    }

    @Test
    fun `appendEvent, revision not sequential`() {
        // Given
        val event1 = NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title")
        val event2 = NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 3, title = "Title")
        var r = createInstance()
        r.appendEvent(event1)
        r = createInstance()

        // When / Then
        assertThrows<IllegalArgumentException> {
            r.appendEvent(event2)
        }
    }

    @Test
    fun `appendEvent, first revision not 1`() {
        // Given
        val event = NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 2, title = "Title")
        val r = createInstance()

        // When / Then
        assertThrows<IllegalArgumentException> {
            r.appendEvent(event)
        }
    }

    @Test
    fun `appendEvent, first revision negative`() {
        // Given
        val event = NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = -1, title = "Title")
        val r = createInstance()

        // When / Then
        assertThrows<IllegalArgumentException> {
            r.appendEvent(event)
        }
    }

    protected abstract fun createInstance(): EventStore

}