package info.maaskant.wmsnotes.model.synchronization

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
        val pairs = listOf(
                NoteCreatedEvent(eventId = 1, noteId = "note-1", revision = 1, title = "Title 1") to CreateNoteCommand("note-1", "Title 1"),
                NoteDeletedEvent(eventId = 1, noteId = "note-1", revision = 1) to DeleteNoteCommand("note-1")
        )
        return pairs.map { (event, expectedCommand) ->
            DynamicTest.dynamicTest("${event::class.simpleName} to ${expectedCommand::class.simpleName}") {
                assertThat(RemoteEventToLocalCommandMapper().map(event))
                        .isEqualTo(expectedCommand)
            }
        }
    }
}