package info.maaskant.wmsnotes.model.eventrepository

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.serialization.EventSerializer
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.observers.TestObserver
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FileEventRepositoryTest {

    private val events = listOf(
            NoteCreatedEvent(eventId = 1, noteId = "note-1", revision = 1, title = "Title 1") to "DATA1",
            NoteCreatedEvent(eventId = 2, noteId = "note-2", revision = 2, title = "Title 2") to "DATA2",
            NoteCreatedEvent(eventId = 3, noteId = "note-3", revision = 3, title = "Title 3") to "DATA3"
    )

    private val eventSerializer: EventSerializer = mockk()

    @BeforeEach
    fun init() {
        clearMocks(eventSerializer)
        events.forEach {
            every { eventSerializer.serialize(it.first) }.returns(it.second.toByteArray())
            every { eventSerializer.deserialize(it.second.toByteArray()) }.returns(it.first)
        }
    }

    @Test
    fun `check that directory is empty on initialization`() {
        // Given
        val tempDir = createTempDir(this::class.simpleName!!)
        FileEventRepository(tempDir, eventSerializer)

        // Then
        assertThat(tempDir.list()).isEmpty()
    }


    @Test
    fun addEvent() {
        // Given
        val tempDir = createTempDir(this::class.simpleName!!)
        val r = FileEventRepository(tempDir, eventSerializer)

        // When
        r.addEvent(events[0].first)

        // Then
        val expectedEventFile = tempDir.resolve("0000000001")
        assertThat(expectedEventFile).exists()
        assertThat(expectedEventFile.readBytes()).isEqualTo("DATA1".toByteArray())
    }

    @Test
    fun `addEvent, duplicate`() {
        // Given
        val tempDir = createTempDir(this::class.simpleName!!)
        val r = FileEventRepository(tempDir, eventSerializer)
        r.addEvent(events[0].first)

        // When
        val throwable = catchThrowable { r.addEvent(events[0].first) }

        // Then
        assertThat(throwable).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun getEvent() {
        // Given
        val tempDir = createTempDir(this::class.simpleName!!)
        val r = FileEventRepository(tempDir, eventSerializer)
        r.addEvent(events[0].first)

        // When
        val event = r.getEvent(1)

        // Then
        assertThat(event).isEqualTo(events[0].first)
    }

    @Test
    fun getCurrentEvents() {
        // Given
        val tempDir = createTempDir(this::class.simpleName!!)
        val r = FileEventRepository(tempDir, eventSerializer)
        events.forEach {
            r.addEvent(it.first)
        }
        val observer = TestObserver<Event>()

        // When
        r.getCurrentEvents(afterEventId = 1).subscribe(observer)

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(events[1].first, events[2].first))
    }

    @Test
    fun removeEvent() {
        // Given
        val tempDir = createTempDir(this::class.simpleName!!)
        val r = FileEventRepository(tempDir, eventSerializer)
        r.addEvent(events[0].first)

        // When
        r.removeEvent(events[0].first)

        // Then
        val expectedEventFile = tempDir.resolve("0000000001")
        assertThat(expectedEventFile).doesNotExist()
    }

    @Test
    fun `removeEvent, nonexistent`() {
        // Given
        val tempDir = createTempDir(this::class.simpleName!!)
        val r = FileEventRepository(tempDir, eventSerializer)

        // When
        val throwable = catchThrowable { r.removeEvent(events[0].first) }

        // Then
        assertThat(throwable).isInstanceOf(IllegalStateException::class.java)
    }

}