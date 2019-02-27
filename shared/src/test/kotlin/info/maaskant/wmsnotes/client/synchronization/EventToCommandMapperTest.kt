package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal class EventToCommandMapperTest {
    @TestFactory
    fun test(): List<DynamicTest> {
        val noteId = "note-1"
        val lastRevision = 11
        val eventRevision = lastRevision + 1

        val pairs = listOf(
                NoteCreatedEvent(eventId = 1, noteId = noteId, revision = eventRevision, path = TODO(), title = "Title 1", content = TODO()) to CreateNoteCommand(noteId = noteId, path = TODO(), title = "Title 1", content = TODO()),
                NoteDeletedEvent(eventId = 1, noteId = noteId, revision = eventRevision) to DeleteNoteCommand(noteId = noteId, lastRevision = lastRevision),
                NoteUndeletedEvent(eventId = 1, noteId = noteId, revision = eventRevision) to UndeleteNoteCommand(noteId = noteId, lastRevision = lastRevision),
                AttachmentAddedEvent(eventId = 0, noteId = noteId, revision = eventRevision, name = "att-1", content = "data".toByteArray()) to AddAttachmentCommand(noteId = noteId, lastRevision = lastRevision, name = "att-1", content = "data".toByteArray()),
                AttachmentDeletedEvent(eventId = 0, noteId = noteId, revision = eventRevision, name = "att-1") to DeleteAttachmentCommand(noteId = noteId, lastRevision = lastRevision, name = "att-1"),
                ContentChangedEvent(eventId = 0, noteId = noteId, revision = eventRevision, content = "Text") to ChangeContentCommand(noteId = noteId, lastRevision = lastRevision, content = "Text"),
                TitleChangedEvent(eventId = 0, noteId = noteId, revision = eventRevision, title = "Title") to ChangeTitleCommand(noteId = noteId, lastRevision = lastRevision, title = "Title"),
                MovedEvent(eventId = 0, noteId = noteId, revision = eventRevision, path = Path("el1", "el2")) to MoveCommand(noteId = noteId, lastRevision = lastRevision, path = Path("el1", "el2"))
                // Add more classes here
        )
        return pairs.map { (event, expectedCommand) ->
            DynamicTest.dynamicTest("${event::class.simpleName} to ${expectedCommand::class.simpleName}") {
                assertThat(EventToCommandMapper().map(event, lastRevision = lastRevision))
                        .isEqualTo(expectedCommand)
            }
        }
    }
}