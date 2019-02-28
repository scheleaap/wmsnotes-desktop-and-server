package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.folder.*
import info.maaskant.wmsnotes.model.note.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventToCommandMapper @Inject constructor() {
    fun map(source: Event, lastRevision: Int?): Command {
        return when (source) {
            is NoteEvent -> map1(source, lastRevision)
            is FolderEvent -> map2(source, lastRevision)
            else -> throw IllegalArgumentException()
        }
    }

    private fun map2(source: FolderEvent, lastRevision: Int?): FolderCommand {
        return when (source) {
            is FolderCreatedEvent -> TODO()
            is FolderDeletedEvent -> TODO()
        }
    }

    private fun map1(source: NoteEvent, lastRevision: Int?): Command {
        return when (source) {
            is NoteCreatedEvent -> CreateNoteCommand(source.aggId, path = source.path, title = source.title, content = source.content)
            is NoteDeletedEvent -> DeleteNoteCommand(source.aggId, lastRevision!!)
            is NoteUndeletedEvent -> UndeleteNoteCommand(source.aggId, lastRevision!!)
            is AttachmentAddedEvent -> AddAttachmentCommand(source.aggId, lastRevision!!, name = source.name, content = source.content)
            is AttachmentDeletedEvent -> DeleteAttachmentCommand(source.aggId, lastRevision!!, name = source.name)
            is ContentChangedEvent -> ChangeContentCommand(source.aggId, lastRevision!!, content = source.content)
            is TitleChangedEvent -> ChangeTitleCommand(source.aggId, lastRevision!!, title = source.title)
            is MovedEvent -> MoveCommand(source.aggId, lastRevision!!, path = source.path)
        }
    }
}