package info.maaskant.wmsnotes.model.note

import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.CommandToEventMapper
import info.maaskant.wmsnotes.model.Event
import java.util.*
import javax.inject.Singleton

@Singleton
class NoteCommandToEventMapper : CommandToEventMapper<Note> {
    override fun map(source: Command, lastRevision: Int): Event {
        return when (source) {
            is NoteCommand -> map(source)
            else -> throw IllegalArgumentException()
        }
    }

    private fun map(source: NoteCommand): NoteEvent {
        return when (source) {
            is CreateNoteCommand -> NoteCreatedEvent(eventId = 0, aggId = source.aggId, revision = 1, path = source.path, title = source.title, content = source.content)
            is DeleteNoteCommand -> NoteDeletedEvent(eventId = 0, aggId = source.aggId, revision = source.lastRevision + 1)
            is UndeleteNoteCommand -> NoteUndeletedEvent(eventId = 0, aggId = source.aggId, revision = source.lastRevision + 1)
            is AddAttachmentCommand -> AttachmentAddedEvent(eventId = 0, aggId = source.aggId, revision = source.lastRevision + 1, name = source.name, content = source.content)
            is DeleteAttachmentCommand -> AttachmentDeletedEvent(eventId = 0, aggId = source.aggId, revision = source.lastRevision + 1, name = source.name)
            is ChangeContentCommand -> ContentChangedEvent(eventId = 0, aggId = source.aggId, revision = source.lastRevision + 1, content = source.content)
            is ChangeTitleCommand -> TitleChangedEvent(eventId = 0, aggId = source.aggId, revision = source.lastRevision + 1, title = source.title)
            is MoveCommand -> MovedEvent(eventId = 0, aggId = source.aggId, revision = source.lastRevision + 1, path = source.path)
        }
    }
}
