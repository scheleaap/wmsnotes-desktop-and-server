package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.note.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.*

internal class CommandToEventMapperTest {

    @TestFactory
    fun test(): List<DynamicTest> {
        val aggId = "note-1"
        val lastRevision = 11
        val eventRevision = lastRevision + 1

        val pairs = listOf(
                CreateNoteCommand(aggId = aggId, path = Path("el1", "el2"), title = "Title 1", content = "Text 1") to NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = Path("el1", "el2"), title = "Title 1", content = "Text 1"),
                DeleteNoteCommand(aggId = aggId, lastRevision = lastRevision) to NoteDeletedEvent(eventId = 0, aggId = aggId, revision = eventRevision),
                UndeleteNoteCommand(aggId = aggId, lastRevision = lastRevision) to NoteUndeletedEvent(eventId = 0, aggId = aggId, revision = eventRevision),
                AddAttachmentCommand(aggId = aggId, lastRevision = lastRevision, name = "att-1", content = "data".toByteArray()) to AttachmentAddedEvent(eventId = 0, aggId = aggId, revision = eventRevision, name = "att-1", content = "data".toByteArray()),
                DeleteAttachmentCommand(aggId = aggId, lastRevision = lastRevision, name = "att-1") to AttachmentDeletedEvent(eventId = 0, aggId = aggId, revision = eventRevision, name = "att-1"),
                ChangeContentCommand(aggId = aggId, lastRevision = lastRevision, content = "Text") to ContentChangedEvent(eventId = 0, aggId = aggId, revision = eventRevision, content = "Text"),
                ChangeTitleCommand(aggId = aggId, lastRevision = lastRevision, title = "Title") to TitleChangedEvent(eventId = 0, aggId = aggId, revision = eventRevision, title = "Title"),
                MoveCommand(aggId = aggId, lastRevision = lastRevision, path = Path("el1", "el2")) to MovedEvent(eventId = 0, aggId = aggId, revision = eventRevision, path = Path("el1", "el2"))
                // Add more classes here
        )
        return pairs.map { (command, expectedEvent) ->
            DynamicTest.dynamicTest("${command::class.simpleName} to ${expectedEvent::class.simpleName}") {
                assertThat(CommandToEventMapper().map(command)).isEqualTo(expectedEvent)
            }
        }
    }

    @Test
    fun `create, aggregate id null`() {
        // Given
        val command = CreateNoteCommand(null, path = Path("el"), title = "Title 1", content = "Text 1")

        // When
        val event = CommandToEventMapper().map(command)

        // Then
        UUID.fromString(event.aggId) // Expected not to throw an exception
    }

}