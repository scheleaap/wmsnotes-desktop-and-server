package info.maaskant.wmsnotes.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.*

internal class CommandToEventMapperTest {

    @TestFactory
    fun test(): List<DynamicTest> {
        val noteId = "note-1"
        val lastRevision = 11
        val eventRevision = lastRevision + 1

        val pairs = listOf(
                CreateNoteCommand(noteId, "Title 1") to NoteCreatedEvent(eventId = 0, noteId = noteId, revision = 1, title = "Title 1"),
                DeleteNoteCommand(noteId, lastRevision) to NoteDeletedEvent(eventId = 0, noteId = noteId, revision = eventRevision),
                UndeleteNoteCommand(noteId, lastRevision) to NoteUndeletedEvent(eventId = 0, noteId = noteId, revision = eventRevision),
                AddAttachmentCommand(noteId, lastRevision, "att-1", "data".toByteArray()) to AttachmentAddedEvent(eventId = 0, noteId = noteId, revision = eventRevision, name = "att-1", content = "data".toByteArray()),
                DeleteAttachmentCommand(noteId, lastRevision, "att-1") to AttachmentDeletedEvent(eventId = 0, noteId = noteId, revision = eventRevision, name = "att-1"),
                ChangeContentCommand(noteId, lastRevision, "data") to ContentChangedEvent(eventId = 0, noteId = noteId, revision = eventRevision, content = "data"),
                ChangeTitleCommand(noteId, lastRevision, "Title") to TitleChangedEvent(eventId = 0, noteId = noteId, revision = eventRevision, title = "Title")
                // Add more classes here
        )
        return pairs.map { (command, expectedEvent) ->
            DynamicTest.dynamicTest("${command::class.simpleName} to ${expectedEvent::class.simpleName}") {
                assertThat(CommandToEventMapper().map(command)).isEqualTo(expectedEvent)
            }
        }
    }

    @Test
    fun `create, note id null`() {
        // Given
        val command = CreateNoteCommand(null, "Title 1")

        // When
        val event = CommandToEventMapper().map(command)

        // Then
        UUID.fromString(event.noteId) // Expected not to throw an exception
    }

}