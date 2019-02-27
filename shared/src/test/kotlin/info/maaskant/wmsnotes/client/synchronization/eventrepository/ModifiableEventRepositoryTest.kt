package info.maaskant.wmsnotes.client.synchronization.eventrepository

import info.maaskant.wmsnotes.model.NoteCreatedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal abstract class ModifiableEventRepositoryTest {

    protected val events = listOf(
            NoteCreatedEvent(eventId = 1, noteId = "note-1", revision = 1, path = TODO(), title = "Title 1", content = TODO()) to "DATA1",
            NoteCreatedEvent(eventId = 2, noteId = "note-2", revision = 2, path = TODO(), title = "Title 2", content = TODO()) to "DATA2",
            NoteCreatedEvent(eventId = 3, noteId = "note-3", revision = 3, path = TODO(), title = "Title 3", content = TODO()) to "DATA3"
    )

    @Test
    fun `addEvent, duplicate`() {
        // Given
        val r = createInstance()
        r.addEvent(events[0].first)

        // When / Then
        assertThrows<IllegalArgumentException> {
            r.addEvent(events[0].first)
        }
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
    fun getEvents() {
        // Given
        val r = createInstance()
        events.forEach {
            r.addEvent(it.first)
        }

        // When
        val observer = r.getEvents().test()

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(events[0].first, events[1].first, events[2].first))
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

        // When / Then
        assertThrows<IllegalArgumentException> {
            r.removeEvent(events[0].first)
        }
    }

    protected abstract fun createInstance(): ModifiableEventRepository

}