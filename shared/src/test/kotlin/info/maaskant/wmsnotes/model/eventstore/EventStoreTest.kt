package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
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
                givenAnEvent(1, NoteCreatedEvent(eventId = 0, noteId = "note-3", revision = 1, title = "Title 3")),
                givenAnEvent(2, NoteCreatedEvent(eventId = 0, noteId = "note-2", revision = 1, title = "Title 2")),
                givenAnEvent(3, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title 1")),
                givenAnEvent(4, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 2, title = "Title 1"))
        )
        val eventsOut = listOf(
                eventsIn[1].withEventId(2),
                eventsIn[2].withEventId(3),
                eventsIn[3].withEventId(4)
        )
        var r: EventStore = createInstance()
        eventsIn.forEach {
            r.appendEvent(it)
            r = createInstance()
        }

        // When
        val observer = r.getEvents(afterEventId = 1).test()

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values()).isEqualTo(eventsOut)
    }

    @Test
    fun `appendEvent, revision already exists`() {
        // Given
        val event1 = givenAnEvent(1, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title"))
        val event2 = givenAnEvent(2, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title"))
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
        val event1 = givenAnEvent(1, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title"))
        val event2 = givenAnEvent(2, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 3, title = "Title"))
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

    @Test
    fun `get note events, without updates, no filtering`() {
        // Given
        val eventsIn = listOf(
                givenAnEvent(1, NoteCreatedEvent(eventId = 0, noteId = "note-3", revision = 1, title = "Title 3")),
                givenAnEvent(2, NoteCreatedEvent(eventId = 0, noteId = "note-2", revision = 1, title = "Title 2")),
                givenAnEvent(3, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title 1")),
                givenAnEvent(4, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 2, title = "Title 1"))
        )
        val eventsOut = listOf(
                eventsIn[2].withEventId(3)
        )
        var r: EventStore = createInstance()
        eventsIn.subList(0, 3).forEach {
            r.appendEvent(it)
            r = createInstance()
        }

        // When
        val observer = r.getEventsOfNote("note-1").test()
        r.appendEvent(eventsIn[3])

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values()).isEqualTo(eventsOut)
    }

    @Test
    fun `get note events, without updates, filter by revision`() {
        // Given
        val eventsIn = listOf(
                givenAnEvent(1, NoteCreatedEvent(eventId = 0, noteId = "note-3", revision = 1, title = "Title 3")),
                givenAnEvent(2, NoteCreatedEvent(eventId = 0, noteId = "note-2", revision = 1, title = "Title 2")),
                givenAnEvent(3, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title 1")),
                givenAnEvent(4, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 2, title = "Title 1")),
                givenAnEvent(5, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 3, title = "Title 1"))
        )
        val eventsOut = listOf(
                eventsIn[3].withEventId(4)
        )
        var r: EventStore = createInstance()
        eventsIn.subList(0, 4).forEach {
            r.appendEvent(it)
            r = createInstance()
        }

        // When
        val observer = r.getEventsOfNote("note-1", afterRevision = 1).test()
        r.appendEvent(eventsIn[4])

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values()).isEqualTo(eventsOut)
    }


    @Test
    fun `get note events, with updates, no filtering`() {
        // Given
        val eventsIn = listOf(
                givenAnEvent(1, NoteCreatedEvent(eventId = 0, noteId = "note-3", revision = 1, title = "Title 3")),
                givenAnEvent(2, NoteCreatedEvent(eventId = 0, noteId = "note-2", revision = 1, title = "Title 2")),
                givenAnEvent(3, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title 1")),
                givenAnEvent(4, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 2, title = "Title 1"))
        )
        val eventsOut = listOf(
                eventsIn[2].withEventId(3),
                eventsIn[3].withEventId(4)
        )
        var r: EventStore = createInstance()
        eventsIn.subList(0, 3).forEach {
            r.appendEvent(it)
            r = createInstance()
        }

        // When
        val observer = r.getEventsOfNoteWithUpdates("note-1").test()
        r.appendEvent(eventsIn[3])

        // Then
        observer.assertNotComplete()
        observer.assertNotTerminated()
        observer.assertNoErrors()
        assertThat(observer.values()).isEqualTo(eventsOut)
    }

    @Test
    fun `get note events, with updates, filter by revision`() {
        // Given
        val eventsIn = listOf(
                givenAnEvent(1, NoteCreatedEvent(eventId = 0, noteId = "note-3", revision = 1, title = "Title 3")),
                givenAnEvent(2, NoteCreatedEvent(eventId = 0, noteId = "note-2", revision = 1, title = "Title 2")),
                givenAnEvent(3, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title 1")),
                givenAnEvent(4, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 2, title = "Title 1")),
                givenAnEvent(5, NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 3, title = "Title 1"))
        )
        val eventsOut = listOf(
                eventsIn[3].withEventId(4),
                eventsIn[4].withEventId(5)
        )
        var r: EventStore = createInstance()
        eventsIn.subList(0, 4).forEach {
            r.appendEvent(it)
            r = createInstance()
        }

        // When
        val observer = r.getEventsOfNoteWithUpdates("note-1", afterRevision = 1).test()
        r.appendEvent(eventsIn[4])

        // Then
        observer.assertNotComplete()
        observer.assertNotTerminated()
        observer.assertNoErrors()
        assertThat(observer.values()).isEqualTo(eventsOut)
    }

    @Test
    fun `getEventUpdates, initially`() {
        // Given
        val r: EventStore = createInstance()
        r.appendEvent(NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title 1"))

        // When
        val observer = r.getEventUpdates().test()

        // Then
        observer.assertValueCount(0)
        observer.assertNotComplete()
        observer.assertNoErrors()
    }

    @Test
    fun `getEventUpdates, new event`() {
        // Given
        val event1In = NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title 1")
        val event2In = NoteCreatedEvent(eventId = 0, noteId = "note-2", revision = 1, title = "Title 2")
        val event2Out = NoteCreatedEvent(eventId = 2, noteId = event2In.noteId, revision = event2In.revision, title = event2In.title)
        val r: EventStore = createInstance()
        r.appendEvent(event1In)
        val observer = r.getEventUpdates().test()

        // When
        r.appendEvent(event2In)

        // Then
        observer.assertValueCount(1)
        observer.assertValues(event2Out)
        observer.assertNotComplete()
        observer.assertNoErrors()
    }

    protected abstract fun createInstance(): EventStore

    protected abstract fun <T : Event> givenAnEvent(eventId: Int, event: T): T
}