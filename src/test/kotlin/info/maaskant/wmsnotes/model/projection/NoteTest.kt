package info.maaskant.wmsnotes.model.projection

import info.maaskant.wmsnotes.model.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.util.*

@Suppress("RemoveRedundantBackticks")
internal class NoteTest {

    private val randomNoteId = UUID.randomUUID().toString()
    private val binaryData = "data".toByteArray()

    @TestFactory
    fun `wrong revision`(): List<DynamicTest> {
        return listOf(
                NoteDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 3),
                AttachmentAddedEvent(eventId = 0, noteId = randomNoteId, revision = 3, name = "file", content = "data".toByteArray()),
                AttachmentDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 3, name = "file")
                // Add more classes here
        ).map { event ->
            DynamicTest.dynamicTest(event::class.simpleName) {
                // Given
                val note = noteWithEvents(NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"))

                // When / Then
                assertThat(event.revision).isGreaterThan(note.revision + 1) // Validate the test data
                assertThrows<IllegalArgumentException> { note.apply(event) }
            }
        }
    }

    @TestFactory
    fun `wrong note id`(): List<DynamicTest> {
        return listOf(
                NoteDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 2),
                AttachmentAddedEvent(eventId = 0, noteId = randomNoteId, revision = 2, name = "file", content = "data".toByteArray()),
                AttachmentDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 2, name = "file")
                // Add more classes here
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
                NoteDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 1),
                AttachmentAddedEvent(eventId = 0, noteId = randomNoteId, revision = 1, name = "file", content = "data".toByteArray()),
                AttachmentDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 1, name = "file")
                // Add more classes here
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

    @TestFactory
    fun `events that are not allowed if the note does not exist`(): List<DynamicTest> {
        return listOf(
                AttachmentAddedEvent(eventId = 0, noteId = randomNoteId, revision = 3, name = "file", content = "data".toByteArray())
                // Add more classes here
        ).map { event ->
            DynamicTest.dynamicTest(event::class.simpleName) {
                // Given
                val note = noteWithEvents(
                        NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"),
                        NoteDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 2)
                )

                // When / Then
                assertThat(event.revision).isEqualTo(3)
                assertThrows<IllegalStateException> { note.apply(event) }
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
        assertThat(noteBefore.revision).isEqualTo(0)
        assertThat(noteBefore.exists).isEqualTo(false)
        assertThat(noteAfter.revision).isEqualTo(eventIn.revision)
        assertThat(noteAfter.exists).isEqualTo(true)
        assertThat(noteAfter.noteId).isEqualTo(eventIn.noteId)
        assertThat(noteAfter.title).isEqualTo(eventIn.title)
        assertThat(noteAfter.attachments).isEqualTo(emptyMap<String, ByteArray>())
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
        assertThat(noteBefore.revision).isEqualTo(1)
        assertThat(noteBefore.exists).isEqualTo(true)
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
        assertThat(noteAfter).isEqualTo(noteBefore)
    }

    @Test
    fun `add attachment`() {
        // Given
        val noteBefore = noteWithEvents(NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"))
        val eventIn = AttachmentAddedEvent(eventId = 0, noteId = randomNoteId, revision = 2, name = "att", content = binaryData)

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(noteBefore.revision).isEqualTo(1)
        assertThat(noteBefore.attachments).isEqualTo(emptyMap<String, ByteArray>())
        assertThat(noteAfter.revision).isEqualTo(2)
        assertThat(noteAfter.attachments).isEqualTo(mapOf("att" to binaryData))
    }

    @Test
    fun `add attachment, idempotence`() {
        // Given
        val noteBefore = noteWithEvents(
                NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"),
                AttachmentAddedEvent(eventId = 0, noteId = randomNoteId, revision = 2, name = "att", content = binaryData)
        )
        val eventIn = AttachmentAddedEvent(eventId = 0, noteId = randomNoteId, revision = 3, name = "att", content = binaryData)

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(noteAfter).isEqualTo(noteBefore)
    }

    @Test
    fun `add attachment, duplicate`() {
        // Given
        val noteBefore = noteWithEvents(
                NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"),
                AttachmentAddedEvent(eventId = 0, noteId = randomNoteId, revision = 2, name = "att", content = binaryData)
        )
        val eventIn = AttachmentAddedEvent(eventId = 0, noteId = randomNoteId, revision = 3, name = "att", content = "different".toByteArray())

        // When / Then
        assertThrows<IllegalStateException> { noteBefore.apply(eventIn) }
    }

    @Test
    fun `delete attachment`() {
        // Given
        val noteBefore = noteWithEvents(
                NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"),
                AttachmentAddedEvent(eventId = 0, noteId = randomNoteId, revision = 2, name = "att", content = binaryData)
        )
        val eventIn = AttachmentDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 3, name = "att")

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(noteBefore.revision).isEqualTo(2)
        assertThat(noteBefore.attachments).isEqualTo(mapOf("att" to binaryData))
        assertThat(noteAfter.revision).isEqualTo(3)
        assertThat(noteAfter.attachments).isEqualTo(emptyMap<String, ByteArray>())
    }
    @Test
    fun `delete attachment, idempotence`() {
        // Given
        val noteBefore = noteWithEvents(
                NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"),
                AttachmentAddedEvent(eventId = 0, noteId = randomNoteId, revision = 2, name = "att", content = binaryData)
        )
        val eventIn = AttachmentDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 3, name = "some other")

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(noteAfter).isEqualTo(noteBefore)
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
