package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.utilities.serialization.Serializer
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

internal class FileEventStoreTest : EventStoreTest() {
    private val noteId = "n-10000000-0000-0000-0000-000000000000"

    private lateinit var tempDir: File
    private var eventSerializer: Serializer<Event> = mockk()

    @BeforeEach
    fun init() {
        tempDir = createTempDir(this::class.simpleName!!).resolve("events")
        clearMocks(
                eventSerializer
        )
        every { eventSerializer.serialize(any()) }.returns(UUID.randomUUID().toString().toByteArray())
    }

    @Test
    fun `appendEvent, check file`() {
        // Given
        val eventIn = NoteCreatedEvent(eventId = 0, aggId = "note", revision = 1, path = Path("path"), title = "Title", content = "Text")
        val eventOut = eventIn.copy(eventId = 1)
        val r = createInstance()

        // When
        r.appendEvent(eventIn)

        // Then
        val expectedEventFile = tempDir.resolve("note").resolve("0000000001")
        assertThat(expectedEventFile).exists()
        assertThat(expectedEventFile.readBytes()).isEqualTo(eventSerializer.serialize(eventOut))
    }

    @Test
    fun getAggregateIds() {
        // Given
        val event1 = NoteCreatedEvent(eventId = 0, aggId = noteId, revision = 1, path = Path("path"), title = "Title", content = "Text")
        val event2 = FolderCreatedEvent(eventId = 0,  revision = 1, path = Path("path"))
        val r = createInstance()
        r.appendEvent(event1)
        r.appendEvent(event2)

        // When
        val observer = r.getAggregateIds().test()

        // Then
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values().toSet()).isEqualTo(setOf(event1.aggId, event2.aggId))
    }

    override fun createInstance(): FileEventStore {
        return FileEventStore(tempDir, eventSerializer)
    }

    override fun <T : Event> givenAnEvent(eventId: Int, event: T): T {
        val content = UUID.randomUUID().toString().toByteArray()
        val event2 = event.copy(eventId = eventId)
        every { eventSerializer.serialize(event2) }.returns(content)
        every { eventSerializer.deserialize(content) }.returns(event2)
        return event
    }

}