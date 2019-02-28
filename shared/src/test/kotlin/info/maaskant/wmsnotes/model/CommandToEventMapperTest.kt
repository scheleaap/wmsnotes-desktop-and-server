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
                CreateNoteCommand(noteId = noteId, path = Path("el1", "el2"), title = "Title 1", content = "Text 1") to NoteCreatedEvent(eventId = 0, noteId = noteId, revision = 1, path = Path("el1","el2"), title = "Title 1", content = "Text 1"),
                DeleteNoteCommand(noteId = noteId, lastRevision = lastRevision) to NoteDeletedEvent(eventId = 0, noteId = noteId, revision = eventRevision),
                UndeleteNoteCommand(noteId = noteId, lastRevision = lastRevision) to NoteUndeletedEvent(eventId = 0, noteId = noteId, revision = eventRevision),
                AddAttachmentCommand(noteId = noteId, lastRevision = lastRevision, name = "att-1", content = "data".toByteArray()) to AttachmentAddedEvent(eventId = 0, noteId = noteId, revision = eventRevision, name = "att-1", content = "data".toByteArray()),
                DeleteAttachmentCommand(noteId = noteId, lastRevision = lastRevision, name = "att-1") to AttachmentDeletedEvent(eventId = 0, noteId = noteId, revision = eventRevision, name = "att-1"),
                ChangeContentCommand(noteId = noteId, lastRevision = lastRevision, content = "Text") to ContentChangedEvent(eventId = 0, noteId = noteId, revision = eventRevision, content = "Text"),
                ChangeTitleCommand(noteId = noteId, lastRevision = lastRevision, title = "Title") to TitleChangedEvent(eventId = 0, noteId = noteId, revision = eventRevision, title = "Title"),
                MoveCommand(noteId = noteId, lastRevision = lastRevision, path = Path("el1", "el2")) to MovedEvent(eventId = 0, noteId = noteId, revision = eventRevision, path = Path("el1", "el2"))
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
        val command = CreateNoteCommand(null, path = Path("el"), title = "Title 1", content = "Text 1")

        // When
        val event = CommandToEventMapper().map(command)

        // Then
        UUID.fromString(event.noteId) // Expected not to throw an exception
    }

}