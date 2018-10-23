package info.maaskant.wmsnotes.model.projection.cache

import info.maaskant.wmsnotes.model.projection.Note
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

internal class FileNoteCacheTest : NoteCacheTest() {
    val data: ByteArray = "DATA".toByteArray()

    private lateinit var tempDir: File

    private val noteSerializer: NoteSerializer = mockk()

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
        val expectedFile = tempDir.resolve(noteId).resolve("0000000002")
        assertThat(expectedFile).exists()
        assertThat(expectedFile.readBytes()).isEqualTo(noteSerializer.serialize(note))
    }

    @Test
    fun `remove, check file`() {
        // Given
        val note: Note = noteAfterEvent2
        val c = createInstance()
        c.put(note)

        // When
        c.remove(note.noteId, note.revision)

        // Then
        val expectedFile = tempDir.resolve(noteId).resolve("0000000002")
        assertThat(expectedFile).doesNotExist()
    }

    override fun createInstance(): NoteCache {
        return FileNoteCache(tempDir, noteSerializer)
    }

    override fun givenANote(note: Note): Note {
        val content = UUID.randomUUID().toString().toByteArray()
        every { noteSerializer.serialize(note) }.returns(content)
        every { noteSerializer.deserialize(content) }.returns(note)
        return note
    }

}