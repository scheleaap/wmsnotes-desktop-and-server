package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventToCommandMapper @Inject constructor() {
    fun map(source: Event, lastRevision: Int?): Command {
        return when (source) {
            is NoteCreatedEvent -> CreateNoteCommand(source.noteId, source.title)
            is NoteDeletedEvent -> DeleteNoteCommand(source.noteId, lastRevision!!)
            is AttachmentAddedEvent -> AddAttachmentCommand(source.noteId, lastRevision!!, source.name, source.content)
            is AttachmentDeletedEvent -> DeleteAttachmentCommand(source.noteId, lastRevision!!, source.name)
            is ContentChangedEvent -> ChangeContentCommand(source.noteId, lastRevision!!, source.content)
        }
    }
}