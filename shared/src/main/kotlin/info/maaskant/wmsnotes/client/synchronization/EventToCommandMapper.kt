package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventToCommandMapper @Inject constructor() {
    fun map(source: Event, lastRevision: Int?): Command {
        return when (source) {
            is NoteCreatedEvent -> CreateNoteCommand(source.noteId, path = source.path, title = source.title, content = source.content)
            is NoteDeletedEvent -> DeleteNoteCommand(source.noteId, lastRevision!!)
            is NoteUndeletedEvent -> UndeleteNoteCommand(source.noteId, lastRevision!!)
            is AttachmentAddedEvent -> AddAttachmentCommand(source.noteId, lastRevision!!, name = source.name, content = source.content)
            is AttachmentDeletedEvent -> DeleteAttachmentCommand(source.noteId, lastRevision!!, name = source.name)
            is ContentChangedEvent -> ChangeContentCommand(source.noteId, lastRevision!!, content = source.content)
            is TitleChangedEvent -> ChangeTitleCommand(source.noteId, lastRevision!!, title = source.title)
            is MovedEvent -> MoveCommand(source.noteId, lastRevision!!, path = source.path)
        }
    }
}