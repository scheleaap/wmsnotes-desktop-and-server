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
                NoteCreatedEvent(eventId = 1, noteId = noteId, revision = eventRevision, title = "Title 1") to CreateNoteCommand(noteId, "Title 1"),
                NoteDeletedEvent(eventId = 1, noteId = noteId, revision = eventRevision) to DeleteNoteCommand(noteId, lastRevision),
                AttachmentAddedEvent(eventId = 0, noteId = noteId, revision = eventRevision, name = "att-1", content = "data".toByteArray()) to AddAttachmentCommand(noteId, lastRevision, "att-1", "data".toByteArray()),
                AttachmentDeletedEvent(eventId = 0, noteId = noteId, revision = eventRevision, name = "att-1") to DeleteAttachmentCommand(noteId, lastRevision, "att-1")
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