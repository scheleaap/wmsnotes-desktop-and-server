package info.maaskant.wmsnotes.model.note

import info.maaskant.wmsnotes.model.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.util.*

@Suppress("RemoveRedundantBackticks")
internal class NoteTest {

    private val randomAggId = UUID.randomUUID().toString()
    private val binaryData = "data".toByteArray()
    private val dataHash = "8d777f385d3dfec8815d20f7496026dc"

    @TestFactory
    fun `wrong revision`(): List<DynamicTest> {
        return listOf(
                NoteDeletedEvent(eventId = 0, aggId = randomAggId, revision = 3),
                NoteUndeletedEvent(eventId = 0, aggId = randomAggId, revision = 3),
                AttachmentAddedEvent(eventId = 0, aggId = randomAggId, revision = 3, name = "file", content = "data".toByteArray()),
                AttachmentDeletedEvent(eventId = 0, aggId = randomAggId, revision = 3, name = "file"),
                ContentChangedEvent(eventId = 0, aggId = randomAggId, revision = 3, content = "Text"),
                TitleChangedEvent(eventId = 0, aggId = randomAggId, revision = 3, title = "Title"),
                MovedEvent(eventId = 0, aggId = randomAggId, revision = 3, path = Path("el1", "el2"))
                // Add more classes here
        ).map { event ->
            DynamicTest.dynamicTest(event::class.simpleName) {
                // Given
                val note = noteWithEvents(NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"))

                // When / Then
                assertThat(event.revision).isGreaterThan(note.revision + 1) // Validate the test data
                assertThrows<IllegalArgumentException> { note.apply(event) }
            }
        }
    }

    @TestFactory
    fun `wrong aggregate id`(): List<DynamicTest> {
        return listOf(
                NoteDeletedEvent(eventId = 0, aggId = randomAggId, revision = 2),
                NoteUndeletedEvent(eventId = 0, aggId = randomAggId, revision = 2),
                AttachmentAddedEvent(eventId = 0, aggId = randomAggId, revision = 2, name = "file", content = "data".toByteArray()),
                AttachmentDeletedEvent(eventId = 0, aggId = randomAggId, revision = 2, name = "file"),
                ContentChangedEvent(eventId = 0, aggId = randomAggId, revision = 2, content = "Text"),
                TitleChangedEvent(eventId = 0, aggId = randomAggId, revision = 2, title = "Title"),
                MovedEvent(eventId = 0, aggId = randomAggId, revision = 2, path = Path("el1", "el2"))
                // Add more classes here
        ).map { event ->
            DynamicTest.dynamicTest(event::class.simpleName) {
                // Given
                val note = noteWithEvents(NoteCreatedEvent(eventId = 0, aggId = "original id", revision = 1, path = Path("el"), title = "Title", content = "Text"))

                // When / Then
                assertThat(event.aggId).isNotEqualTo(note.aggId)
                assertThrows<IllegalArgumentException> { note.apply(event) }
            }
        }
    }

    @TestFactory
    fun `events that are not allowed as first event`(): List<DynamicTest> {
        return listOf(
                NoteDeletedEvent(eventId = 0, aggId = randomAggId, revision = 1),
                NoteUndeletedEvent(eventId = 0, aggId = randomAggId, revision = 1),
                AttachmentAddedEvent(eventId = 0, aggId = randomAggId, revision = 1, name = "file", content = "data".toByteArray()),
                AttachmentDeletedEvent(eventId = 0, aggId = randomAggId, revision = 1, name = "file"),
                ContentChangedEvent(eventId = 0, aggId = randomAggId, revision = 1, content = "Text"),
                TitleChangedEvent(eventId = 0, aggId = randomAggId, revision = 1, title = "Title"),
                MovedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el1", "el2"))
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
    fun `events that are allowed if the note has been deleted`(): List<DynamicTest> {
        return listOf(
                AttachmentAddedEvent(eventId = 0, aggId = randomAggId, revision = 3, name = "file", content = "data".toByteArray()),
                AttachmentDeletedEvent(eventId = 0, aggId = randomAggId, revision = 3, name = "file"),
                ContentChangedEvent(eventId = 0, aggId = randomAggId, revision = 3, content = "Text"),
                TitleChangedEvent(eventId = 0, aggId = randomAggId, revision = 3, title = "Title"),
                MovedEvent(eventId = 0, aggId = randomAggId, revision = 3, path = Path("el1", "el2"))
                // Add more classes here
        ).map { eventIn ->
            DynamicTest.dynamicTest(eventIn::class.simpleName) {
                // Given
                assertThat(eventIn.revision).isEqualTo(3)
                val noteBefore = noteWithEvents(
                        NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"),
                        NoteDeletedEvent(eventId = 0, aggId = randomAggId, revision = 2)
                )

                // When
                noteBefore.apply(eventIn)

                // Then
                // no exception thrown
            }
        }
    }

    @Test
    fun `after instantiation`() {
        // When
        val note = Note()

        // Then
        assertThat(note.revision).isEqualTo(0)
        assertThat(note.exists).isEqualTo(false)
        assertThat(note.path).isEqualTo(Path())
        assertThat(note.title).isEmpty()
        assertThat(note.content).isEmpty()
    }

    @Test
    fun `create`() {
        // Given
        val noteBefore = Note()
        val eventIn = NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el1", "el2"), title = "Title", content = "Text")

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(noteBefore.revision).isEqualTo(0)
        assertThat(noteBefore.exists).isEqualTo(false)
        assertThat(noteAfter.revision).isEqualTo(eventIn.revision)
        assertThat(noteAfter.exists).isEqualTo(true)
        assertThat(noteAfter.aggId).isEqualTo(eventIn.aggId)
        assertThat(noteAfter.path).isEqualTo(eventIn.path)
        assertThat(noteAfter.title).isEqualTo(eventIn.title)
        assertThat(noteAfter.content).isEqualTo(eventIn.content)
        assertThat(noteAfter.attachments).isEqualTo(emptyMap<String, ByteArray>())
        assertThat(noteAfter.attachmentHashes).isEqualTo(emptyMap<String, String>())
    }

    @TestFactory
    fun `create, revision not 1`(): List<DynamicTest> {
        return listOf(-1, 0, 2).map { revision ->
            DynamicTest.dynamicTest(revision.toString()) {
                // Given
                val note = Note()
                val eventIn = NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = revision, path = Path("el"), title = "Title", content = "Text")

                // When / Then
                assertThrows<IllegalArgumentException> { note.apply(eventIn) }
            }
        }
    }

    @Test
    fun `create, duplicate`() {
        // Given
        val noteBefore = noteWithEvents(NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"))
        val eventIn = NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 2, path = Path("el"), title = "Title", content = "Text")

        // When / Then
        assertThrows<IllegalStateException> { noteBefore.apply(eventIn) }
    }

    @Test
    fun `create, aggregate id blank string`() {
        // Given
        val noteBefore = Note()
        val eventIn = NoteCreatedEvent(eventId = 0, aggId = " \t", revision = 1, path = Path("el"), title = "Title", content = "Text")

        // When / Then
        assertThrows<IllegalArgumentException> { noteBefore.apply(eventIn) }
    }

    @Test
    fun `delete`() {
        // Given
        val noteBefore = noteWithEvents(NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"))
        val eventIn = NoteDeletedEvent(eventId = 0, aggId = randomAggId, revision = 2)

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
                NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"),
                NoteDeletedEvent(eventId = 0, aggId = randomAggId, revision = 2)
        )
        val eventIn = NoteDeletedEvent(eventId = 0, aggId = randomAggId, revision = 3)

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(noteAfter).isEqualTo(noteBefore)
    }

    @Test
    fun `undelete`() {
        // Given
        val noteBefore = noteWithEvents(
                NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"),
                NoteDeletedEvent(eventId = 0, aggId = randomAggId, revision = 2)
        )
        val eventIn = NoteUndeletedEvent(eventId = 0, aggId = randomAggId, revision = 3)

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(noteBefore.revision).isEqualTo(2)
        assertThat(noteBefore.exists).isEqualTo(false)
        assertThat(noteAfter.revision).isEqualTo(3)
        assertThat(noteAfter.exists).isEqualTo(true)
    }

    @Test
    fun `undelete, idempotence`() {
        // Given
        val noteBefore = noteWithEvents(
                NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"),
                NoteDeletedEvent(eventId = 0, aggId = randomAggId, revision = 2),
                NoteUndeletedEvent(eventId = 0, aggId = randomAggId, revision = 3)
        )
        val eventIn = NoteUndeletedEvent(eventId = 0, aggId = randomAggId, revision = 4)

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(noteAfter).isEqualTo(noteBefore)
    }

    @Test
    fun `undelete, ignore after create`() {
        // Given
        val noteBefore = noteWithEvents(
                NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text")
        )
        val eventIn = NoteUndeletedEvent(eventId = 0, aggId = randomAggId, revision = 2)

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(noteAfter).isEqualTo(noteBefore)
    }

    @Test
    fun `add attachment`() {
        // Given
        val noteBefore = noteWithEvents(NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"))
        val eventIn = AttachmentAddedEvent(eventId = 0, aggId = randomAggId, revision = 2, name = "att", content = binaryData)

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
                NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"),
                AttachmentAddedEvent(eventId = 0, aggId = randomAggId, revision = 2, name = "att", content = binaryData)
        )
        val eventIn = AttachmentAddedEvent(eventId = 0, aggId = randomAggId, revision = 3, name = "att", content = binaryData)

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
                NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"),
                AttachmentAddedEvent(eventId = 0, aggId = randomAggId, revision = 2, name = "att", content = binaryData)
        )
        val eventIn = AttachmentAddedEvent(eventId = 0, aggId = randomAggId, revision = 3, name = "att", content = "different".toByteArray())

        // When / Then
        assertThrows<IllegalStateException> { noteBefore.apply(eventIn) }
    }

    @Test
    fun `add attachment, special characters in name`() {
        // Given
        val noteBefore = noteWithEvents(NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"))
        val eventIn = AttachmentAddedEvent(eventId = 0, aggId = randomAggId, revision = 2, name = "att \t\\/&.jpg", content = binaryData)

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
                NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text")
        )
        val eventIn = AttachmentAddedEvent(eventId = 0, aggId = randomAggId, revision = 2, name = "", content = binaryData)

        // When / Then
        assertThrows<IllegalArgumentException> { noteBefore.apply(eventIn) }
    }

    @Test
    fun `delete attachment`() {
        // Given
        val noteBefore = noteWithEvents(
                NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"),
                AttachmentAddedEvent(eventId = 0, aggId = randomAggId, revision = 2, name = "att", content = binaryData)
        )
        val eventIn = AttachmentDeletedEvent(eventId = 0, aggId = randomAggId, revision = 3, name = "att")

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
                NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"),
                AttachmentAddedEvent(eventId = 0, aggId = randomAggId, revision = 2, name = "att", content = binaryData)
        )
        val eventIn = AttachmentDeletedEvent(eventId = 0, aggId = randomAggId, revision = 3, name = "some other")

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(noteAfter).isEqualTo(noteBefore)
    }

    @Test
    fun `content changed`() {
        // Given
        val noteBefore = noteWithEvents(NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text 1"))
        val eventIn = ContentChangedEvent(eventId = 0, aggId = randomAggId, revision = 2, content = "Text 2")

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(noteBefore.revision).isEqualTo(1)
        assertThat(noteBefore.content).isEqualTo("Text 1")
        assertThat(noteAfter.revision).isEqualTo(2)
        assertThat(noteAfter.content).isEqualTo("Text 2")
    }

    @Test
    fun `content changed, idempotence 1`() {
        // Given
        val noteBefore = noteWithEvents(NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"))
        val eventIn = ContentChangedEvent(eventId = 0, aggId = randomAggId, revision = 2, content = "Text")

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(noteAfter).isEqualTo(noteBefore)
    }

    @Test
    fun `content changed, idempotence 2`() {
        // Given
        val noteBefore = noteWithEvents(
                NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text 1"),
                ContentChangedEvent(eventId = 0, aggId = randomAggId, revision = 2, content = "Text 2")
        )
        val eventIn = ContentChangedEvent(eventId = 0, aggId = randomAggId, revision = 3, content = "Text 2")

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(noteAfter).isEqualTo(noteBefore)
    }

    @Test
    fun `title changed`() {
        // Given
        val noteBefore = noteWithEvents(NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title 1", content = "Text"))
        val eventIn = TitleChangedEvent(eventId = 0, aggId = randomAggId, revision = 2, title = "Title 2")

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(noteBefore.revision).isEqualTo(1)
        assertThat(noteBefore.title).isEqualTo("Title 1")
        assertThat(noteAfter.revision).isEqualTo(2)
        assertThat(noteAfter.title).isEqualTo("Title 2")
    }

    @Test
    fun `title changed, idempotence 1`() {
        // Given
        val noteBefore = noteWithEvents(NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"))
        val eventIn = TitleChangedEvent(eventId = 0, aggId = randomAggId, revision = 2, title = "Title")

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(noteAfter).isEqualTo(noteBefore)
    }

    @Test
    fun `title changed, idempotence 2`() {
        // Given
        val noteBefore = noteWithEvents(
                NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"),
                TitleChangedEvent(eventId = 0, aggId = randomAggId, revision = 2, title = "Title 2")
        )
        val eventIn = TitleChangedEvent(eventId = 0, aggId = randomAggId, revision = 3, title = "Title 2")

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(noteAfter).isEqualTo(noteBefore)
    }

    @Test
    fun moved() {
        // Given
        val noteBefore = noteWithEvents(NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el1"), title = "Title", content = "Text"))
        val eventIn = MovedEvent(eventId = 0, aggId = randomAggId, revision = 2, path = Path("el2"))

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(noteBefore.revision).isEqualTo(1)
        assertThat(noteBefore.path).isEqualTo(Path("el1"))
        assertThat(noteAfter.revision).isEqualTo(2)
        assertThat(noteAfter.path).isEqualTo(Path("el2"))
    }

    @Test
    fun `moved, idempotence 1`() {
        // Given
        val noteBefore = noteWithEvents(NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el"), title = "Title", content = "Text"))
        val eventIn = MovedEvent(eventId = 0, aggId = randomAggId, revision = 2, path = Path("el"))

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(noteAfter).isEqualTo(noteBefore)
    }

    @Test
    fun `moved, idempotence 2`() {
        // Given
        val noteBefore = noteWithEvents(
                NoteCreatedEvent(eventId = 0, aggId = randomAggId, revision = 1, path = Path("el1"), title = "Title", content = "Text"),
                MovedEvent(eventId = 0, aggId = randomAggId, revision = 2, path = Path("el2"))
        )
        val eventIn = MovedEvent(eventId = 0, aggId = randomAggId, revision = 3, path = Path("el2"))

        // When
        val (noteAfter, eventOut) = noteBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(noteAfter).isEqualTo(noteBefore)
    }

    // Add more classes here

    @TestFactory
    fun `equals and hashCode for all fields`(): List<DynamicTest> {
        val revision = 0
        val exists = false
        val aggId = ""
        val path = Path("path")
        val title = "Title"
        val content = "Text"
        val attachments = emptyMap<String, ByteArray>()
        val attachmentHashes = emptyMap<String, String>()
        val original = Note.deserialize(revision = revision, exists = exists, aggId = aggId, path = path, title = title, content = content, attachments = attachments, attachmentHashes = attachmentHashes)
        return listOf(
                "revision" to Note.deserialize(revision = 1, exists = exists, aggId = aggId, path = path, title = title, content = content, attachments = attachments, attachmentHashes = attachmentHashes),
                "exists" to Note.deserialize(revision = revision, exists = true, aggId = aggId, path = path, title = title, content = content, attachments = attachments, attachmentHashes = attachmentHashes),
                "aggId" to Note.deserialize(revision = revision, exists = exists, aggId = "different", path = path, title = title, content = content, attachments = attachments, attachmentHashes = attachmentHashes),
                "path" to Note.deserialize(revision = revision, exists = exists, aggId = aggId, path = Path("different"), title = title, content = content, attachments = attachments, attachmentHashes = attachmentHashes),
                "title" to Note.deserialize(revision = revision, exists = exists, aggId = aggId, path = path, title = "different", content = content, attachments = attachments, attachmentHashes = attachmentHashes),
                "content" to Note.deserialize(revision = revision, exists = exists, aggId = aggId, path = path, title = title, content = "different", attachments = attachments, attachmentHashes = attachmentHashes),
                "attachmentHashes" to Note.deserialize(revision = revision, exists = exists, aggId = aggId, path = path, title = title, content = content, attachments = attachments, attachmentHashes = mapOf("different" to "different"))
                // "" to Note.deserialize(revision = revision, exists = exists, aggId = aggId, title = title, content = content, attachments = attachments, attachmentHashes = attachmentHashes),
                // Add more fields here
        ).map {
            DynamicTest.dynamicTest(it.first) {
                // Given
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

    companion object {
        private fun noteWithEvents(vararg events: NoteEvent): Note {
            var note = Note()
            for (event in events) {
                val (newNote, _) = note.apply(event)
                note = newNote
            }
            return note
        }
    }
}
