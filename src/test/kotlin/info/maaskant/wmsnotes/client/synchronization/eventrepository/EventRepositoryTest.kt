package info.maaskant.wmsnotes.client.synchronization.eventrepository

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import io.reactivex.observers.TestObserver
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test

internal abstract class EventRepositoryTest {

    protected val events = listOf(
            NoteCreatedEvent(eventId = 1, noteId = "note-1", revision = 1, title = "Title 1") to "DATA1",
            NoteCreatedEvent(eventId = 2, noteId = "note-2", revision = 2, title = "Title 2") to "DATA2",
            NoteCreatedEvent(eventId = 3, noteId = "note-3", revision = 3, title = "Title 3") to "DATA3"
    )

    @Test
    fun `addEvent, duplicate`() {
        // Given
        val r = createInstance()
        r.addEvent(events[0].first)

        // When
        val throwable = catchThrowable { r.addEvent(events[0].first) }

        // Then
        assertThat(throwable).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun getEvent() {
        // Given
        val r = createInstance()
        r.addEvent(events[0].first)

        // When
        val event = r.getEvent(1)

        // Then
        assertThat(event).isEqualTo(events[0].first)
    }

    @Test
    fun getCurrentEvents() {
        // Given
        val r = createInstance()
        events.forEach {
            r.addEvent(it.first)
        }
        val observer = TestObserver<Event>()

        // When
        r.getEvents(afterEventId = 1).subscribe(observer)

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(events[1].first, events[2].first))
    }

    @Test
    fun removeEvent() {
        // Given
        val r = createInstance()
        r.addEvent(events[0].first)

        // When
        r.removeEvent(events[0].first)
        val event = r.getEvent(1)

        // Then
        assertThat(event).isNull()
    }

    @Test
    fun `removeEvent, nonexistent`() {
        // Given
        val r = createInstance()

        // When
        val throwable = catchThrowable { r.removeEvent(events[0].first) }

        // Then
        assertThat(throwable).isInstanceOf(IllegalStateException::class.java)
    }

    protected abstract fun createInstance(): ModifiableEventRepository

}