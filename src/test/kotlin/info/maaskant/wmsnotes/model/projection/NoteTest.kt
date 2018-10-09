package info.maaskant.wmsnotes.model.projection

import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.util.*

@Suppress("RemoveRedundantBackticks")
internal class NoteTest {

    private val randomNoteId = UUID.randomUUID().toString()

    @TestFactory
    fun `wrong revision`(): List<DynamicTest> {
        return listOf(
                NoteDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 3)
                // Add more event types here
        ).map { event ->
            DynamicTest.dynamicTest(event::class.simpleName) {
                // Given
                val note = Note()
                note.apply(NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"))

                // When / Then
                assertThat(event.revision).isGreaterThan(note.revision + 1)
                assertThrows<IllegalArgumentException> { note.apply(event) }
                assertThat(note.revision).isEqualTo(1)
                assertThat(note.exists).isEqualTo(true)
            }
        }
    }

    @TestFactory
    fun `wrong note id`(): List<DynamicTest> {
        return listOf(
                NoteDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 2)
                // Add more event types here
        ).map { event ->
            DynamicTest.dynamicTest(event::class.simpleName) {
                // Given
                val note = Note()
                note.apply(NoteCreatedEvent(eventId = 0, noteId = "original id", revision = 1, title = "Title"))

                // When / Then
                assertThat(event.noteId).isNotEqualTo(note.noteId)
                assertThrows<IllegalArgumentException> { note.apply(event) }
                assertThat(note.revision).isEqualTo(1)
                assertThat(note.exists).isEqualTo(true)
                assertThat(note.noteId).isEqualTo("original id")
            }
        }
    }

    @Test
    fun `after construction`() {
        // When
        val note = Note()

        // Then
        assertThat(note.revision).isEqualTo(0)
        assertThat(note.exists).isEqualTo(false)
    }

    @Test
    fun `create`() {
        // Given
        val note = Note()
        val eventIn = NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title")

        // When
        val eventOut = note.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(note.revision).isEqualTo(eventIn.revision)
        assertThat(note.exists).isEqualTo(true)
        assertThat(note.noteId).isEqualTo(eventIn.noteId)
        assertThat(note.title).isEqualTo(eventIn.title)
    }

    @TestFactory
    fun `create, revision not 1`(): List<DynamicTest> {
        return listOf(-1, 0, 2).map { revision ->
            DynamicTest.dynamicTest(revision.toString()) {
                // Given
                val note = Note()
                val eventIn = NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = revision, title = "Title")

                // When / Then
                assertThrows<IllegalArgumentException> { note.apply(eventIn) }
            }
        }
    }

    @Test
    fun `create, duplicate`() {
        // Given
        val note = Note()
        note.apply(NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"))
        val eventIn = NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 2, title = "Title")

        // When / Then
        assertThrows<IllegalStateException> { note.apply(eventIn) }
        assertThat(note.revision).isEqualTo(1)
    }

    @Test
    fun `delete`() {
        // Given
        val note = Note()
        note.apply(NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"))
        val eventIn = NoteDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 2)

        // When
        val eventOut = note.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(note.revision).isEqualTo(2)
        assertThat(note.exists).isEqualTo(false)
    }

    @Test
    fun `delete, idempotence`() {
        // Given
        val note = Note()
        note.apply(NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"))
        note.apply(NoteDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 2))
        val eventIn = NoteDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 3)

        // When
        val eventOut = note.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(note.revision).isEqualTo(2)
        assertThat(note.exists).isEqualTo(false)
    }
}