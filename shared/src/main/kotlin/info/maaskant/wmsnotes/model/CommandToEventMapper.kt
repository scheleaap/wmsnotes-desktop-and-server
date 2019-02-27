package info.maaskant.wmsnotes.model

import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandToEventMapper @Inject constructor() {
    fun map(command: Command): Event {
        return when (command) {
            is CreateNoteCommand -> {
                val noteId = command.noteId ?: UUID.randomUUID().toString()
                NoteCreatedEvent(eventId = 0, noteId = noteId, revision = 1, path=TODO(),title = command.title,content = TODO())
            }
            is DeleteNoteCommand -> NoteDeletedEvent(eventId = 0, noteId = command.noteId, revision = command.lastRevision + 1)
            is UndeleteNoteCommand -> NoteUndeletedEvent(eventId = 0, noteId = command.noteId, revision = command.lastRevision + 1)
            is AddAttachmentCommand -> AttachmentAddedEvent(eventId = 0, noteId = command.noteId, revision = command.lastRevision + 1, name = command.name, content = command.content)
            is DeleteAttachmentCommand -> AttachmentDeletedEvent(eventId = 0, noteId = command.noteId, revision = command.lastRevision + 1, name = command.name)
            is ChangeContentCommand -> ContentChangedEvent(eventId = 0, noteId = command.noteId, revision = command.lastRevision + 1, content = command.content)
            is ChangeTitleCommand -> TitleChangedEvent(eventId = 0, noteId = command.noteId, revision = command.lastRevision + 1, title = command.title)
            is MoveCommand -> TODO()
        }
    }
}
