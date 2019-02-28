package info.maaskant.wmsnotes.model

import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandToEventMapper @Inject constructor() {
    fun map(source: Command): Event {
        return when (source) {
            is CreateNoteCommand -> {
                val noteId = source.noteId ?: UUID.randomUUID().toString()
                NoteCreatedEvent(eventId = 0, noteId = noteId, revision = 1, path = source.path, title = source.title, content = source.content)
            }
            is DeleteNoteCommand -> NoteDeletedEvent(eventId = 0, noteId = source.noteId, revision = source.lastRevision + 1)
            is UndeleteNoteCommand -> NoteUndeletedEvent(eventId = 0, noteId = source.noteId, revision = source.lastRevision + 1)
            is AddAttachmentCommand -> AttachmentAddedEvent(eventId = 0, noteId = source.noteId, revision = source.lastRevision + 1, name = source.name, content = source.content)
            is DeleteAttachmentCommand -> AttachmentDeletedEvent(eventId = 0, noteId = source.noteId, revision = source.lastRevision + 1, name = source.name)
            is ChangeContentCommand -> ContentChangedEvent(eventId = 0, noteId = source.noteId, revision = source.lastRevision + 1, content = source.content)
            is ChangeTitleCommand -> TitleChangedEvent(eventId = 0, noteId = source.noteId, revision = source.lastRevision + 1, title = source.title)
            is MoveCommand -> MovedEvent(eventId = 0, noteId = source.noteId, revision = source.lastRevision + 1, path = source.path)
        }
    }
}
