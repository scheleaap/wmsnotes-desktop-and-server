package info.maaskant.wmsnotes.model.eventrepository

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.serialization.EventSerializer
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.observers.TestObserver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FileEventRepositoryTest {

    private val events = listOf(
            NoteCreatedEvent(eventId = 1, noteId = "note-1", title = "Title 1") to "DATA1",
            NoteCreatedEvent(eventId = 2, noteId = "note-2", title = "Title 2") to "DATA2",
            NoteCreatedEvent(eventId = 3, noteId = "note-3", title = "Title 3") to "DATA3"
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
    fun storeEvent() {
        // Given
        val tempDir = createTempDir(this::class.simpleName!!)
        val r = FileEventRepository(tempDir, eventSerializer)

        // When
        val completable = r.storeEvent(events[0].first)

        // Then
        assertThat(tempDir.list()).isEmpty()

        // When
        val t = completable.blockingGet()

        // Then
        assertThat(t).isNull()
        val expectedEventFile = tempDir.resolve("0000000001")
        assertThat(expectedEventFile).exists()
        assertThat(expectedEventFile.readBytes()).isEqualTo("DATA1".toByteArray())
    }

    @Test
    fun getEvent() {
        // Given
        val tempDir = createTempDir(this::class.simpleName!!)
        val r = FileEventRepository(tempDir, eventSerializer)
            assertThat(r.storeEvent(events[0].first).blockingGet()).isNull()
        val observer = TestObserver<Event>()

        // When
        r.getEvent(1).subscribe(observer)

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values()[0]).isEqualTo(events[0].first)
    }

    @Test
    fun getEvents() {
        // Given
        val tempDir = createTempDir(this::class.simpleName!!)
        val r = FileEventRepository(tempDir, eventSerializer)
        events.forEach {
            assertThat(r.storeEvent(it.first).blockingGet()).isNull()
        }
        val observer = TestObserver<Event>()

        // When
        r.getEvents(afterEventId = 1).subscribe(observer)

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(events[1].first, events[2].first))
    }
}