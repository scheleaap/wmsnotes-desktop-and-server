package info.maaskant.wmsnotes.model.note

import info.maaskant.wmsnotes.model.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.*

internal class NoteCommandToEventMapperTest {
    private val path = Path("el1", "el2")
    private val title = "Title"
    private val content = "Text"

    @TestFactory
    fun test(): List<DynamicTest> {
        val aggId = "note-1"
        val lastRevision = 11
        val eventRevision = lastRevision + 1

        val pairs = listOf(
                CreateNoteCommand(aggId = aggId, path = path, title = title, content = content) to NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = path, title = title, content = content),
                DeleteNoteCommand(aggId = aggId, lastRevision = lastRevision) to NoteDeletedEvent(eventId = 0, aggId = aggId, revision = eventRevision),
                UndeleteNoteCommand(aggId = aggId, lastRevision = lastRevision) to NoteUndeletedEvent(eventId = 0, aggId = aggId, revision = eventRevision),
                AddAttachmentCommand(aggId = aggId, lastRevision = lastRevision, name = "att-1", content = "data".toByteArray()) to AttachmentAddedEvent(eventId = 0, aggId = aggId, revision = eventRevision, name = "att-1", content = "data".toByteArray()),
                DeleteAttachmentCommand(aggId = aggId, lastRevision = lastRevision, name = "att-1") to AttachmentDeletedEvent(eventId = 0, aggId = aggId, revision = eventRevision, name = "att-1"),
                ChangeContentCommand(aggId = aggId, lastRevision = lastRevision, content = content) to ContentChangedEvent(eventId = 0, aggId = aggId, revision = eventRevision, content = content),
                ChangeTitleCommand(aggId = aggId, lastRevision = lastRevision, title = title) to TitleChangedEvent(eventId = 0, aggId = aggId, revision = eventRevision, title = title),
                MoveCommand(aggId = aggId, lastRevision = lastRevision, path = path) to MovedEvent(eventId = 0, aggId = aggId, revision = eventRevision, path = path)
                // Add more classes here
        )
        return pairs.map { (command, expectedEvent) ->
            DynamicTest.dynamicTest("${command::class.simpleName} to ${expectedEvent::class.simpleName}") {
                assertThat(NoteCommandToEventMapper().map(command)).isEqualTo(expectedEvent)
            }
        }
    }

    @Test
    fun `create, aggregate id null`() {
        // Given
        val command = CreateNoteCommand(null, path = path, title = title, content = content)

        // When
        val event = NoteCommandToEventMapper().map(command)

        // Then
        assertThat(event.aggId).startsWith("n-")
        UUID.fromString(event.aggId.substring(startIndex = 2)) // Expected not to throw an exception
    }

}