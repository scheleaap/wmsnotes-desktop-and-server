package info.maaskant.wmsnotes.model.folder

import au.com.console.kassava.kotlinToString
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path

sealed class FolderEvent(eventId: Int, revision: Int, path: Path) : Event(eventId, path.toString(), revision)

class FolderCreatedEvent(eventId: Int, revision: Int, val path: Path) : FolderEvent(eventId, revision, path) {
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

class FolderDeletedEvent(eventId: Int, revision: Int, val path: Path) : FolderEvent(eventId, revision, path) {
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