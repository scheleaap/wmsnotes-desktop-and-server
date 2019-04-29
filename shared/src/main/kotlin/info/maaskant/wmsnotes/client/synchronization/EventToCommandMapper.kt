package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.folder.*
import info.maaskant.wmsnotes.model.note.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventToCommandMapper @Inject constructor() {
    fun map(source: Event): Command {
        return when (source) {
            is NoteEvent -> map(source)
            is FolderEvent -> map(source)
            else -> throw IllegalArgumentException()
        }
    }

    private fun map(source: FolderEvent): FolderCommand {
        return when (source) {
            is FolderCreatedEvent -> CreateFolderCommand(path = source.path)
            is FolderDeletedEvent -> DeleteFolderCommand(path = source.path)
        }
    }

    private fun map(source: NoteEvent): Command {
        return when (source) {
            is NoteCreatedEvent -> CreateNoteCommand(source.aggId, path = source.path, title = source.title, content = source.content)
            is NoteDeletedEvent -> DeleteNoteCommand(source.aggId)
            is NoteUndeletedEvent -> UndeleteNoteCommand(source.aggId)
            is AttachmentAddedEvent -> AddAttachmentCommand(source.aggId, name = source.name, content = source.content)
            is AttachmentDeletedEvent -> DeleteAttachmentCommand(source.aggId, name = source.name)
            is ContentChangedEvent -> ChangeContentCommand(source.aggId, content = source.content)
            is TitleChangedEvent -> ChangeTitleCommand(source.aggId, title = source.title)
            is MovedEvent -> MoveCommand(source.aggId, path = source.path)
        }
    }
}