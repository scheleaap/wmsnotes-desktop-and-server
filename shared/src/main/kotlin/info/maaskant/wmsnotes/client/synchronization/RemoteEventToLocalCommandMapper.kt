package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.*
import javax.inject.Singleton

@Singleton
class RemoteEventToLocalCommandMapper {
    fun map(source: Event, lastRevision: Int?): Command {
        return when (source) {
            is NoteCreatedEvent -> CreateNoteCommand(source.noteId, source.title)
            is NoteDeletedEvent -> DeleteNoteCommand(source.noteId, lastRevision!!)
            is AttachmentAddedEvent -> AddAttachmentCommand(source.noteId, lastRevision!!, source.name, source.content)
            is AttachmentDeletedEvent -> DeleteAttachmentCommand(source.noteId, lastRevision!!, source.name)
        }
    }
}