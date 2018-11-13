package info.maaskant.wmsnotes.model.projection

import info.maaskant.wmsnotes.model.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Fail.fail
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.util.*

@Suppress("RemoveRedundantBackticks")
internal class NoteTest {

    private val randomNoteId = UUID.randomUUID().toString()
    private val binaryData = "data".toByteArray()
    private val dataHash = "8d777f385d3dfec8815d20f7496026dc"

    @TestFactory
    fun `wrong revision`(): List<DynamicTest> {
        return listOf(
                NoteDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 3),
                AttachmentAddedEvent(eventId = 0, noteId = randomNoteId, revision = 3, name = "file", content = "data".toByteArray()),
                AttachmentDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 3, name = "file"),
                ContentChangedEvent(eventId = 0, noteId = randomNoteId, revision = 3, content = "data")
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
                AttachmentDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 2, name = "file"),
                ContentChangedEvent(eventId = 0, noteId = randomNoteId, revision = 2, content = "data")
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
                AttachmentDeletedEvent(eventId = 0, noteId = randomNoteId, revision = 1, name = "file"),
                ContentChangedEvent(eventId = 0, noteId = randomNoteId, revision = 1, content = "data")
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
                AttachmentAddedEvent(eventId = 0, noteId = randomNoteId, revision = 3, name = "file", content = "data".toByteArray()),
                ContentChangedEvent(eventId = 0, noteId = randomNoteId, revision = 3, content = "data")
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
        assertThat(noteAfter.attachmentHashes).isEqualTo(emptyMap<String, String>())
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
        assertThat(noteBefore.attachmentHashes).isEqualTo(emptyMap<String, String>())
        assertThat(noteAfter.revision).isEqualTo(2)
        assertThat(noteAfter.attachments).isEqualTo(mapOf("att" to binaryData))
        assertThat(noteAfter.attachmentHashes).isEqualTo(mapOf("att" to dataHash))
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
    fun `add attachment, special characters in name`() {
        // Given
        val noteBefore = noteWithEvents(NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"))
        val eventIn = AttachmentAddedEvent(eventId = 0, noteId = randomNoteId, revision = 2, name = "att \t\\/&.jpg", content = binaryData)

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(noteAfter.attachments.keys).isEqualTo(setOf("att_____.jpg"))
    }

    @Test
    fun `add attachment, empty name`() {
        // Given
        val noteBefore = noteWithEvents(
                NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title")
        )
        val eventIn = AttachmentAddedEvent(eventId = 0, noteId = randomNoteId, revision = 2, name = "", content = binaryData)

        // When / Then
        assertThrows<IllegalArgumentException> { noteBefore.apply(eventIn) }
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
        assertThat(noteBefore.attachmentHashes).isEqualTo(mapOf("att" to dataHash))
        assertThat(noteAfter.revision).isEqualTo(3)
        assertThat(noteAfter.attachments).isEqualTo(emptyMap<String, ByteArray>())
        assertThat(noteAfter.attachmentHashes).isEqualTo(emptyMap<String, String>())
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

    @Test
    fun `content changed`() {
        // Given
        val noteBefore = noteWithEvents(NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"))
        val eventIn = ContentChangedEvent(eventId = 0, noteId = randomNoteId, revision = 2, content = "data")

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(noteBefore.revision).isEqualTo(1)
        assertThat(noteBefore.content).isEqualTo("")
        assertThat(noteAfter.revision).isEqualTo(2)
        assertThat(noteAfter.content).isEqualTo("data")
    }

    @Test
    fun `content changed, idempotence`() {
        // Given
        val noteBefore = noteWithEvents(
                NoteCreatedEvent(eventId = 0, noteId = randomNoteId, revision = 1, title = "Title"),
                ContentChangedEvent(eventId = 0, noteId = randomNoteId, revision = 2, content = "data")
        )
        val eventIn = ContentChangedEvent(eventId = 0, noteId = randomNoteId, revision = 3, content = "data")

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(noteAfter).isEqualTo(noteBefore)
    }

    @TestFactory
    fun `equals and hashCode for all fields`(): List<DynamicTest> {
        val revision = 0
        val exists = false
        val noteId = ""
        val title = ""
        val content = ""
        val attachments = emptyMap<String, ByteArray>()
        val attachmentHashes = emptyMap<String, String>()
        return listOf(
                "revision" to Note.deserialize(revision = 1, exists = exists, noteId = noteId, title = title, content = content, attachments = attachments, attachmentHashes = attachmentHashes),
                "exists" to Note.deserialize(revision = revision, exists = true, noteId = noteId, title = title, content = content, attachments = attachments, attachmentHashes = attachmentHashes),
                "noteId" to Note.deserialize(revision = revision, exists = exists, noteId = "different", title = title, content = content, attachments = attachments, attachmentHashes = attachmentHashes),
                "title" to Note.deserialize(revision = revision, exists = exists, noteId = noteId, title = "different", content = content, attachments = attachments, attachmentHashes = attachmentHashes),
                "content" to Note.deserialize(revision = revision, exists = exists, noteId = noteId, title = title, content = "different", attachments = attachments, attachmentHashes = attachmentHashes),
                "attachmentHashes" to Note.deserialize(revision = revision, exists = exists, noteId = noteId, title = title, content = content, attachments = attachments, attachmentHashes = mapOf("different" to "different"))
                // "" to Note.deserialize(revision = revision, exists = exists, noteId = noteId, title = title, content = content, attachments = attachments, attachmentHashes = attachmentHashes),
                // Add more fields here
        ).map {
            DynamicTest.dynamicTest(it.first) {
                // Given
                val original = Note()
                val modified = it.second

                // Then
                assertThat(original).isEqualTo(original)
                assertThat(original.hashCode()).isEqualTo(original.hashCode())
                assertThat(modified).isEqualTo(modified)
                assertThat(modified.hashCode()).isEqualTo(modified.hashCode())
                assertThat(original).isNotEqualTo(modified)
            }
        }
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
