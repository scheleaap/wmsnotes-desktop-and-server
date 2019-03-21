package info.maaskant.wmsnotes.model.aggregaterepository

import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.utilities.serialization.Serializer
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

internal class FileAggregateCacheTest : AggregateCacheTest() {
    val data: ByteArray = "DATA".toByteArray()

    private lateinit var tempDir: File

    private val noteSerializer: Serializer<Note> = mockk()

    @BeforeEach
    override fun init() {
        tempDir = createTempDir(this::class.simpleName!!)
        clearMocks(
                noteSerializer
        )
        super.init()
    }

    @Test
    fun `put, check file`() {
        // Given
        val note: Note = noteAfterEvent2
        val c = createInstance()

        // When
        c.put(note)

        // Then
        val expectedFile = tempDir.resolve(aggId).resolve("0000000002")
        assertThat(expectedFile).exists()
        assertThat(expectedFile.readBytes()).isEqualTo(noteSerializer.serialize(note))
    }

    @Test
    fun `put, do not replace if file exists`() {
        // Given
        val note: Note = noteAfterEvent2
        val c = createInstance()
        val aggregateDir = tempDir.resolve(aggId)
        val revisionFile = aggregateDir.resolve("0000000002")
        aggregateDir.mkdirs()
        revisionFile.writeBytes(data)

        // When
        c.put(note)

        // Then
        assertThat(revisionFile).exists()
        assertThat(revisionFile.readBytes()).isEqualTo(data)
    }

    @Test
    fun `remove, check file`() {
        // Given
        val note: Note = noteAfterEvent2
        val c = createInstance()
        c.put(note)

        // When
        c.remove(note.aggId, note.revision)

        // Then
        val expectedFile = tempDir.resolve(aggId).resolve("0000000002")
        assertThat(expectedFile).doesNotExist()
    }

    override fun createInstance(): AggregateCache<Note> {
        return FileAggregateCache(tempDir, noteSerializer)
    }

    override fun givenANote(note: Note): Note {
        val content = UUID.randomUUID().toString().toByteArray()
        every { noteSerializer.serialize(note) }.returns(content)
        every { noteSerializer.deserialize(content) }.returns(note)
        return note
    }

}