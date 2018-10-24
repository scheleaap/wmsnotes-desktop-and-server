package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.utilities.serialization.EventSerializer
import io.mockk.MockKException
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

internal class FileEventStoreTest : EventStoreTest() {

    private lateinit var tempDir: File
    private var eventSerializer: EventSerializer = mockk()

    @BeforeEach
    fun init() {
        tempDir = createTempDir(this::class.simpleName!!)
        clearMocks(
                eventSerializer
        )
        every { eventSerializer.serialize(any()) }.returns(UUID.randomUUID().toString().toByteArray())
    }

    @Test
    fun `check that directory is empty on initialization`() {
        // Given
        val tempDir = createTempDir(this::class.simpleName!!)
        createInstance()

        // Then
        assertThat(tempDir.list()).isEmpty()
    }

    @Test
    fun `appendEvent, check file`() {
        // Given
        val eventIn = NoteCreatedEvent(eventId = 0, noteId = "note", revision = 1, title = "Title")
        val eventOut = eventIn.withEventId(eventId = 1)
        val r = createInstance()

        // When
        r.appendEvent(eventIn)

        // Then
        val expectedEventFile = tempDir.resolve("note").resolve("0000000001")
        assertThat(expectedEventFile).exists()
        assertThat(expectedEventFile.readBytes()).isEqualTo(eventSerializer.serialize(eventOut))
    }


    override fun createInstance(): EventStore {
        return FileEventStore(tempDir, eventSerializer)
    }

    override fun <T : Event> givenAnEvent(eventId: Int, event: T): T {
        val content = UUID.randomUUID().toString().toByteArray()
        val event2 = event.withEventId(eventId)
        every { eventSerializer.serialize(event2) }.returns(content)
        every { eventSerializer.deserialize(content) }.returns(event2)
        return event
    }

}