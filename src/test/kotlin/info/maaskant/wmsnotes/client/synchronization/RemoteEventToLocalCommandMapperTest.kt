package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.CreateNoteCommand
import info.maaskant.wmsnotes.model.DeleteNoteCommand
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal class RemoteEventToLocalCommandMapperTest {

    @TestFactory
    fun test(): List<DynamicTest> {
        val noteId = "note-1"
        val lastRevision = 11
        val eventRevision = lastRevision + 1

        val pairs = listOf(
                NoteCreatedEvent(eventId = 1, noteId = noteId, revision = eventRevision, title = "Title 1") to CreateNoteCommand(noteId, "Title 1"),
                NoteDeletedEvent(eventId = 1, noteId = noteId, revision = eventRevision) to DeleteNoteCommand(noteId, lastRevision),
                // Add more classes here
        )
        return pairs.map { (event, expectedCommand) ->
            DynamicTest.dynamicTest("${event::class.simpleName} to ${expectedCommand::class.simpleName}") {
                assertThat(RemoteEventToLocalCommandMapper().map(event, lastRevision = lastRevision))
                        .isEqualTo(expectedCommand)
            }
        }
    }
}