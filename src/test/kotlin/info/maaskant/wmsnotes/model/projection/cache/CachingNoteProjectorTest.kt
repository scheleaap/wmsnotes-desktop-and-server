package info.maaskant.wmsnotes.model.projection.cache

import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.NoteProjector
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CachingNoteProjectorTest {
    private val noteId = "note"
    private val lastRevision = 12

    private val wrappedProjector: NoteProjector = mockk()
    private val noteCache: CachingNoteProjector.NoteCache = mockk()

    @BeforeEach
    fun init() {
        clearMocks(
                wrappedProjector,
                noteCache
        )
    }

    @Test
    fun `not present in cache`() {
        // Given
        val wrappedNote: Note = mockk()
        every { wrappedProjector.project(noteId, lastRevision) }.returns(wrappedNote)
        every { noteCache.get(noteId, lastRevision) }.returns(null)
        val p = CachingNoteProjector(wrappedProjector, noteCache)

        // When
        val note = p.project(noteId, lastRevision)

        // Then
        assertThat(note).isEqualTo(wrappedNote)
    }

    @Test
    fun `present in cache`() {
        // Given
        val cachedNote: Note = mockk()
        every { wrappedProjector.project(noteId, lastRevision) }.throws(Exception())
        every { noteCache.get(noteId, lastRevision) }.returns(cachedNote)
        val p = CachingNoteProjector(wrappedProjector, noteCache)

        // When
        val note = p.project(noteId, lastRevision)

        // Then
        assertThat(note).isEqualTo(cachedNote)
    }
}