package info.maaskant.wmsnotes.model.folder

import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.CommandToEventMapper
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.folder.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderCommandToEventMapper : CommandToEventMapper<Folder>{
    override fun map(source: Command): Event {
        return when (source) {
            is FolderCommand -> map(source)
            else -> throw IllegalArgumentException()
        }
    }

    private fun map(source: FolderCommand): Event {
        return when (source) {
            is CreateFolderCommand -> FolderCreatedEvent(eventId = 0, revision = source.lastRevision + 1, path = source.path)
            is DeleteFolderCommand -> FolderDeletedEvent(eventId = 0, revision = source.lastRevision + 1, path = source.path)
        }
    }
}
