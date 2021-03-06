package info.maaskant.wmsnotes.model.eventstore

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isEqualTo
import info.maaskant.wmsnotes.model.CommandError
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import info.maaskant.wmsnotes.testutilities.EitherAssertions.isLeft
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal abstract class EventStoreTest {

    @Test
    fun `appendEvent, non-zero event id`() {
        // Given
        val event = modelEvent(eventId = 1, aggId = 1, revision = 1)
        val r = createInstance()

        // When
        val result = r.appendEvent(event)

        // Then
        assertThat(result).isLeft()
    }

    @Test
    fun `appendEvent and getEvents`() {
        // Given
        val eventsIn = listOf(
                givenAnEvent(1, modelEvent(eventId = 0, aggId = 3, revision = 1)),
                givenAnEvent(2, modelEvent(eventId = 0, aggId = 2, revision = 1)),
                givenAnEvent(3, modelEvent(eventId = 0, aggId = 1, revision = 1)),
                givenAnEvent(4, modelEvent(eventId = 0, aggId = 1, revision = 2))
        )
        val eventsOut = listOf(
                eventsIn[1].copy(eventId = 2),
                eventsIn[2].copy(eventId = 3),
                eventsIn[3].copy(eventId = 4)
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
        assertThat(observer.values().toList()).isEqualTo(eventsOut)
    }

    @Test
    fun `appendEvent, revision already exists`() {
        // Given
        val event1 = givenAnEvent(1, modelEvent(eventId = 0, aggId = 1, revision = 1))
        val event2 = givenAnEvent(2, modelEvent(eventId = 0, aggId = 1, revision = 1))
        var r = createInstance()
        r.appendEvent(event1)
        r = createInstance()

        // When
        val result: Either<CommandError.StorageError, Event> = r.appendEvent(event2)

        // Then
        assertThat(result).isLeft()
    }

    @Test
    fun `appendEvent, revision not sequential`() {
        // Given
        val event1 = givenAnEvent(1, modelEvent(eventId = 0, aggId = 1, revision = 1))
        val event2 = givenAnEvent(2, modelEvent(eventId = 0, aggId = 1, revision = 3))
        var r = createInstance()
        r.appendEvent(event1)
        r = createInstance()

        // When
        val result = r.appendEvent(event2)

        // Then
        assertThat(result).isLeft()
    }

    @Test
    fun `appendEvent, first revision not 1`() {
        // Given
        val event = modelEvent(eventId = 0, aggId = 1, revision = 2)
        val r = createInstance()

        // When
        val result = r.appendEvent(event)

        // Then
        assertThat(result).isLeft()
    }

    @Test
    fun `appendEvent, first revision negative`() {
        // Given
        val event = modelEvent(eventId = 0, aggId = 1, revision = -1)
        val r = createInstance()

        // When
        val result = r.appendEvent(event)

        // Then
        assertThat(result).isLeft()
    }

    @Test
    fun `get note events, without updates, no filtering`() {
        // Given
        val eventsIn = listOf(
                givenAnEvent(1, modelEvent(eventId = 0, aggId = 3, revision = 1)),
                givenAnEvent(2, modelEvent(eventId = 0, aggId = 2, revision = 1)),
                givenAnEvent(3, modelEvent(eventId = 0, aggId = 1, revision = 1)),
                givenAnEvent(4, modelEvent(eventId = 0, aggId = 1, revision = 2))
        )
        val eventsOut = listOf(
                eventsIn[2].copy(eventId = 3)
        )
        var r: EventStore = createInstance()
        eventsIn.subList(0, 3).forEach {
            r.appendEvent(it)
            r = createInstance()
        }

        // When
        val observer = r.getEventsOfAggregate("note-1").test()
        r.appendEvent(eventsIn[3])

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(eventsOut)
    }

    @Test
    fun `get note events, without updates, filter by revision`() {
        // Given
        val eventsIn = listOf(
                givenAnEvent(1, modelEvent(eventId = 0, aggId = 3, revision = 1)),
                givenAnEvent(2, modelEvent(eventId = 0, aggId = 2, revision = 1)),
                givenAnEvent(3, modelEvent(eventId = 0, aggId = 1, revision = 1)),
                givenAnEvent(4, modelEvent(eventId = 0, aggId = 1, revision = 2)),
                givenAnEvent(5, modelEvent(eventId = 0, aggId = 1, revision = 3))
        )
        val eventsOut = listOf(
                eventsIn[3].copy(eventId = 4)
        )
        var r: EventStore = createInstance()
        eventsIn.subList(0, 4).forEach {
            r.appendEvent(it)
            r = createInstance()
        }

        // When
        val observer = r.getEventsOfAggregate("note-1", afterRevision = 1).test()
        r.appendEvent(eventsIn[4])

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(eventsOut)
    }


    @Test
    fun `get note events, with updates, no filtering`() {
        // Given
        val eventsIn = listOf(
                givenAnEvent(1, modelEvent(eventId = 0, aggId = 3, revision = 1)),
                givenAnEvent(2, modelEvent(eventId = 0, aggId = 2, revision = 1)),
                givenAnEvent(3, modelEvent(eventId = 0, aggId = 1, revision = 1)),
                givenAnEvent(4, modelEvent(eventId = 0, aggId = 1, revision = 2))
        )
        val eventsOut = listOf(
                eventsIn[2].copy(eventId = 3),
                eventsIn[3].copy(eventId = 4)
        )
        var r: EventStore = createInstance()
        eventsIn.subList(0, 3).forEach {
            r.appendEvent(it)
            r = createInstance()
        }

        // When
        val observer = r.getEventsOfAggregateWithUpdates("note-1").test()
        r.appendEvent(eventsIn[3])

        // Then
        observer.assertNotComplete()
        observer.assertNotTerminated()
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(eventsOut)
    }

    @Test
    fun `get note events, with updates, filter by revision`() {
        // Given
        val eventsIn = listOf(
                givenAnEvent(1, modelEvent(eventId = 0, aggId = 3, revision = 1)),
                givenAnEvent(2, modelEvent(eventId = 0, aggId = 2, revision = 1)),
                givenAnEvent(3, modelEvent(eventId = 0, aggId = 1, revision = 1)),
                givenAnEvent(4, modelEvent(eventId = 0, aggId = 1, revision = 2)),
                givenAnEvent(5, modelEvent(eventId = 0, aggId = 1, revision = 3))
        )
        val eventsOut = listOf(
                eventsIn[3].copy(eventId = 4),
                eventsIn[4].copy(eventId = 5)
        )
        var r: EventStore = createInstance()
        eventsIn.subList(0, 4).forEach {
            r.appendEvent(it)
            r = createInstance()
        }

        // When
        val observer = r.getEventsOfAggregateWithUpdates("note-1", afterRevision = 1).test()
        r.appendEvent(eventsIn[4])

        // Then
        observer.assertNotComplete()
        observer.assertNotTerminated()
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(eventsOut)
    }

    @Test
    fun `getEventUpdates, initially`() {
        // Given
        val r: EventStore = createInstance()
        r.appendEvent(modelEvent(eventId = 0, aggId = 1, revision = 1))

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
        val event1In = modelEvent(eventId = 0, aggId = 1, revision = 1)
        val event2In = modelEvent(eventId = 0, aggId = 2, revision = 1)
        val event2Out = modelEvent(eventId = 2, aggId = 2, revision = 1)
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

    companion object {
        internal fun modelEvent(eventId: Int, aggId: Int, revision: Int): NoteCreatedEvent {
            return NoteCreatedEvent(eventId = eventId, aggId = "note-$aggId", revision = revision, path = Path("path-$aggId"), title = "Title $aggId", content = "Text $aggId")
        }
    }
}