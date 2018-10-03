package info.maaskant.wmsnotes.model.synchronization

import info.maaskant.wmsnotes.model.*
import javax.inject.Singleton

@Singleton
class RemoteEventToLocalCommandMapper {
    fun map(source: Event): Command {
        return when (source) {
            is NoteCreatedEvent -> CreateNoteCommand(source.noteId, source.title)
            is NoteDeletedEvent -> DeleteNoteCommand(source.noteId)
        }
    }
}