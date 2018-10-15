package info.maaskant.wmsnotes.model.projection.cache

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.utilities.serialization.EventSerializer
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

internal class FileNoteCacheTest : NoteCacheTest() {
    val data: ByteArray = "DATA".toByteArray()

    private lateinit var tempDir: File
    private lateinit var noteSerializer: FileNoteCache.NoteSerializer

    @BeforeEach
    fun init() {
        tempDir = createTempDir(this::class.simpleName!!)
        noteSerializer = TestNoteSerializer()
    }

    @Test
    fun `put, check file`() {
        // Given
        val note: Note = createNote(noteId, revision)
        val c = createInstance()

        // When
        c.put(note)

        // Then
        val expectedFile = tempDir.resolve("$noteId.$revision")
        assertThat(expectedFile).exists()
        assertThat(expectedFile.readBytes()).isEqualTo(noteSerializer.serialize(note))
    }

    // TODO
    // Test that file is deleted

    override fun createInstance(): CachingNoteProjector.NoteCache {
        return FileNoteCache(tempDir, noteSerializer)
    }

    private class TestNoteSerializer : FileNoteCache.NoteSerializer {
        private val map: MutableMap<String, Note> = HashMap()

        override fun serialize(note: Note): ByteArray {
            val key = "${note.noteId}-${note.revision}"
            map[key] = note
            return key.toByteArray()
        }

        override fun deserialize(bytes: ByteArray): Note {
            return map[String(bytes)]!!
        }
    }

}