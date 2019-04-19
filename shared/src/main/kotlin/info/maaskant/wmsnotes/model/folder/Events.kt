package info.maaskant.wmsnotes.model.folder

import au.com.console.kassava.kotlinToString
import com.google.common.annotations.VisibleForTesting
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.folder.Folder.Companion.aggId

sealed class FolderEvent @VisibleForTesting constructor(eventId: Int, aggId: String, revision: Int, val path: Path) : Event(eventId, aggId, revision) {
    constructor(eventId: Int, revision: Int, path: Path) : this(eventId, aggId(path), revision, path)
}

class FolderCreatedEvent @VisibleForTesting constructor(eventId: Int, aggId: String, revision: Int, path: Path) : FolderEvent(eventId, aggId, revision, path) {
    constructor(eventId: Int, revision: Int, path: Path) : this(eventId, aggId(path), revision, path)

    override fun copy(eventId: Int, revision: Int): FolderCreatedEvent =
            FolderCreatedEvent(
                    eventId = eventId,
                    revision = revision,
                    path = path
            )

    override fun toString() = kotlinToString(properties = arrayOf(
            FolderCreatedEvent::eventId,
            FolderCreatedEvent::aggId,
            FolderCreatedEvent::revision,
            FolderCreatedEvent::path
    ))

    override fun canEqual(other: Any?) = other is FolderCreatedEvent
}

class FolderDeletedEvent(eventId: Int, revision: Int, path: Path) : FolderEvent(eventId, revision, path) {
    override fun copy(eventId: Int, revision: Int): FolderDeletedEvent =
            FolderDeletedEvent(
                    eventId = eventId,
                    revision = revision,
                    path = path
            )

    override fun toString() = kotlinToString(properties = arrayOf(
            FolderDeletedEvent::eventId,
            FolderDeletedEvent::aggId,
            FolderDeletedEvent::revision,
            FolderDeletedEvent::path
    ))

    override fun canEqual(other: Any?) = other is FolderDeletedEvent
}