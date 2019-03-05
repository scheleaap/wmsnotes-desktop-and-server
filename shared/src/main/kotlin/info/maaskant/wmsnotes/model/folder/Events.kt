package info.maaskant.wmsnotes.model.folder

import au.com.console.kassava.kotlinToString
import com.google.common.hash.Hashing.md5
import com.google.common.hash.Hashing.sha1
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

sealed class FolderEvent(eventId: Int, revision: Int, val path: Path) : Event(eventId, aggId(path), revision) {
    companion object {
        private fun aggId(path: Path): String {
            return String(Hex.encodeHex(DigestUtils.sha1(path.toString())))
        }
    }
}

class FolderCreatedEvent(eventId: Int, revision: Int, path: Path) : FolderEvent(eventId, revision, path) {
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