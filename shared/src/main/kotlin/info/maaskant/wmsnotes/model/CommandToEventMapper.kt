package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.folder.FolderCommand
import info.maaskant.wmsnotes.model.note.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandToEventMapper @Inject constructor() {
    fun map(source: Command): Event {
        return when (source) {
            is NoteCommand -> map(source)
            is FolderCommand -> map(source)
            else -> throw IllegalArgumentException()
        }
    }

    private fun map(source: FolderCommand): Event {
        return TODO()
    }

    private fun map(source: NoteCommand): NoteEvent {
        return when (source) {
            is CreateNoteCommand -> {
                val aggId = source.aggId ?: UUID.randomUUID().toString()
                NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = source.path, title = source.title, content = source.content)
            }
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
