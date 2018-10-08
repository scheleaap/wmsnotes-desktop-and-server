package info.maaskant.wmsnotes.client.synchronization.eventrepository

import info.maaskant.wmsnotes.model.serialization.EventSerializer
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

internal class FileEventRepositoryTest : EventRepositoryTest() {

    private val eventSerializer: EventSerializer = mockk()

    private var tempDir: File? = null

    @BeforeEach
    fun init() {
        clearMocks(eventSerializer)
        events.forEach {
            every { eventSerializer.serialize(it.first) }.returns(it.second.toByteArray())
            every { eventSerializer.deserialize(it.second.toByteArray()) }.returns(it.first)
        }

        tempDir = createTempDir(this::class.simpleName!!)
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
    fun `addEvent, check file`() {
        // Given
        val r = createInstance()

        // When
        r.addEvent(events[0].first)

        // Then
        val expectedEventFile = tempDir!!.resolve("0000000001")
        assertThat(expectedEventFile).exists()
        assertThat(expectedEventFile.readBytes()).isEqualTo("DATA1".toByteArray())
    }


    @Test
    fun `removeEvent, check file`() {
        // Given
        val r = createInstance()
        r.addEvent(events[0].first)

        // When
        r.removeEvent(events[0].first)

        // Then
        val expectedEventFile = tempDir!!.resolve("0000000001")
        assertThat(expectedEventFile).doesNotExist()
    }

    override fun createInstance(): ModifiableEventRepository {
        return FileEventRepository(tempDir!!, eventSerializer)
    }

}