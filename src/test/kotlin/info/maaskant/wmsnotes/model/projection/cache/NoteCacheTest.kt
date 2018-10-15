package info.maaskant.wmsnotes.model.projection.cache

import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.projection.Note
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

internal abstract class NoteCacheTest {
    protected val noteId = "note"
    protected val revision = 12

    private lateinit var tempDir: File

    @Test
    fun `put and get`() {
        // Given
        val noteIn: Note = createNote(noteId, revision)
        val c = createInstance()

        // When
        c.put(noteIn)
        val noteOut = c.get(noteIn.noteId, noteIn.revision)

        // Then
        assertThat(noteOut).isEqualTo(noteIn)
    }

    @Disabled
    @Test
    fun `get, nonexistent`() {
        // Given
        val noteIn: Note = createNote("other", revision)
        val c = createInstance()
        c.put(noteIn)

        // When
        val noteOut = c.get(noteId, revision)

        // Then
        assertThat(noteOut).isNull()
    }

    protected fun createNote(noteId: String, revision: Int): Note {
        return Note()
                .apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = revision, title = "Title"))
                .component1()
    }

    protected abstract fun createInstance(): CachingNoteProjector.NoteCache

}