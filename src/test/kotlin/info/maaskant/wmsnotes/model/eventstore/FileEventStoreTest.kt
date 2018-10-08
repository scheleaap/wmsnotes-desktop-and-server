package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.serialization.EventSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

internal class FileEventStoreTest : EventStoreTest() {

    private var tempDir: File? = null
    private var eventSerializer: TestEventSerializer? = null

    @BeforeEach
    fun init() {
        tempDir = createTempDir(this::class.simpleName!!)
        eventSerializer = TestEventSerializer()
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
        val expectedEventFile = tempDir!!.resolve("note").resolve("0000000001")
        assertThat(expectedEventFile).exists()
        assertThat(expectedEventFile.readBytes()).isEqualTo(eventSerializer!!.serialize(eventOut))
    }


    override fun createInstance(): EventStore {
        return FileEventStore(tempDir!!, eventSerializer!!)
    }

    private class TestEventSerializer : EventSerializer {
        private val map: MutableMap<Int, Event> = HashMap()

        override fun serialize(event: Event): ByteArray {
            map[event.eventId] = event
            return event.eventId.toString().toByteArray()
        }

        override fun deserialize(bytes: ByteArray): Event {
            return map[String(bytes).toInt()]!!
        }
    }

}