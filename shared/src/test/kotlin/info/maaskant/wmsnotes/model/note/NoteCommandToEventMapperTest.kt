package info.maaskant.wmsnotes.model.note

import info.maaskant.wmsnotes.model.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

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
                DeleteNoteCommand(aggId = aggId) to NoteDeletedEvent(eventId = 0, aggId = aggId, revision = eventRevision),
                UndeleteNoteCommand(aggId = aggId) to NoteUndeletedEvent(eventId = 0, aggId = aggId, revision = eventRevision),
                AddAttachmentCommand(aggId = aggId, name = "att-1", content = "data".toByteArray()) to AttachmentAddedEvent(eventId = 0, aggId = aggId, revision = eventRevision, name = "att-1", content = "data".toByteArray()),
                DeleteAttachmentCommand(aggId = aggId, name = "att-1") to AttachmentDeletedEvent(eventId = 0, aggId = aggId, revision = eventRevision, name = "att-1"),
                ChangeContentCommand(aggId = aggId, content = content) to ContentChangedEvent(eventId = 0, aggId = aggId, revision = eventRevision, content = content),
                ChangeTitleCommand(aggId = aggId, title = title) to TitleChangedEvent(eventId = 0, aggId = aggId, revision = eventRevision, title = title),
                MoveCommand(aggId = aggId, path = path) to MovedEvent(eventId = 0, aggId = aggId, revision = eventRevision, path = path)
                // Add more classes here
        )
        return pairs.map { (command, expectedEvent) ->
            DynamicTest.dynamicTest("${command::class.simpleName} to ${expectedEvent::class.simpleName}") {
                assertThat(NoteCommandToEventMapper().map(command, lastRevision = lastRevision)).isEqualTo(expectedEvent)
            }
        }
    }
}