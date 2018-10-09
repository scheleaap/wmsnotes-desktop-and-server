package info.maaskant.wmsnotes.model.projection

import info.maaskant.wmsnotes.model.Event
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
                val note = noteWithEvents(NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"))

                // When / Then
                assertThat(event.revision).isGreaterThan(note.revision + 1)
                assertThrows<IllegalArgumentException> { note.apply(event) }
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
                val note = noteWithEvents(NoteCreatedEvent(eventId = 0, noteId = "original id", revision = 1, title = "Title"))

                // When / Then
                assertThat(event.noteId).isNotEqualTo(note.noteId)
                assertThrows<IllegalArgumentException> { note.apply(event) }
            }
        }
    }

    @TestFactory
    fun `events that are not allowed as first event`(): List<DynamicTest> {
        return listOf(
                NoteDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 1)
                // Add more event types here
        ).map { event ->
            DynamicTest.dynamicTest(event::class.simpleName) {
                // Given
                val note = Note()

                // When / Then
                assertThat(event.revision).isEqualTo(1)
                assertThrows<IllegalArgumentException> { note.apply(event) }
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
        val noteBefore = Note()
        val eventIn = NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title")

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(noteAfter.revision).isEqualTo(eventIn.revision)
        assertThat(noteAfter.exists).isEqualTo(true)
        assertThat(noteAfter.noteId).isEqualTo(eventIn.noteId)
        assertThat(noteAfter.title).isEqualTo(eventIn.title)
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
        val noteBefore = noteWithEvents(NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"))
        val eventIn = NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 2, title = "Title")

        // When / Then
        assertThrows<IllegalStateException> { noteBefore.apply(eventIn) }
    }

    @Test
    fun `create, note id blank string`() {
        // Given
        val noteBefore = Note()
        val eventIn = NoteCreatedEvent(eventId = 0, noteId = " \t", revision = 1, title = "Title")

        // When / Then
        assertThrows<IllegalArgumentException> { noteBefore.apply(eventIn) }
    }

    @Test
    fun `delete`() {
        // Given
        val noteBefore = noteWithEvents(NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"))
        val eventIn = NoteDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 2)

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(noteAfter.revision).isEqualTo(2)
        assertThat(noteAfter.exists).isEqualTo(false)
    }

    @Test
    fun `delete, idempotence`() {
        // Given
        val noteBefore = noteWithEvents(
                NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"),
                NoteDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 2)
        )
        val eventIn = NoteDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 3)

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(noteAfter.revision).isEqualTo(2)
        assertThat(noteAfter.exists).isEqualTo(false)
    }

}

private fun noteWithEvents(vararg events: Event): Note {
    var note = Note()
    for (event in events) {
        val (newNote, _) = note.apply(event)
        note = newNote
    }
    return note
}
