package info.maaskant.wmsnotes.model.folder

import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinToString
import info.maaskant.wmsnotes.model.Aggregate
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.util.*

class Folder private constructor(
        override val revision: Int,
        val exists: Boolean,
        override val aggId: String,
        val path: Path
) : Aggregate<Folder> {
    constructor() : this(
            revision = 0,
            exists = false,
            aggId = "",
            path = Path()
    )

    val title: String = path.elements.lastOrNull() ?: ""

    override fun equals(other: Any?) = kotlinEquals(other = other, properties = arrayOf(Folder::aggId, Folder::revision, Folder::exists, Folder::path))
    override fun hashCode() = Objects.hash(aggId, revision, exists, path)
    override fun toString() = kotlinToString(properties = arrayOf(Folder::path, Folder::aggId, Folder::revision, Folder::exists))

    private fun copy(
            revision: Int = this.revision,
            exists: Boolean = this.exists,
            aggId: String = this.aggId,
            path: Path = this.path
    ): Folder {
        return Folder(
                revision = revision,
                exists = exists,
                aggId = aggId,
                path = path
        )
    }

    override fun apply(event: Event): Pair<Folder, Event?> {
        if (revision != 0 && event.aggId != aggId) throw IllegalArgumentException("The aggregate id of $event must be $aggId")
        val expectedRevision = revision + 1
        if (event.revision != expectedRevision) throw IllegalArgumentException("The revision of $event must be $expectedRevision")
        return when (event) {
            is FolderEvent -> when (event) {
                is FolderCreatedEvent -> applyCreated(event)
                is FolderDeletedEvent -> applyDeleted(event)
            }
            else -> throw IllegalArgumentException("Wrong event type $event")
        }
    }

    private fun applyCreated(event: FolderCreatedEvent): Pair<Folder, Event?> {
        val expectedAggId = aggId(event.path)
        if (revision == 0 && event.aggId != expectedAggId) throw IllegalArgumentException("The aggregate id of $event must be $expectedAggId")
        return if (!exists) {
            copy(
                    aggId = event.aggId,
                    revision = event.revision,
                    path = event.path,
                    exists = true
            ) to event
        } else {
            noChanges()
        }
    }

    private fun applyDeleted(event: FolderDeletedEvent): Pair<Folder, Event?> {
        if (revision == 0) throw IllegalArgumentException("$event cannot be a note's first event")
        return if (exists) {
            copy(
                    revision = event.revision,
                    exists = false
            ) to event
        } else {
            noChanges()
        }
    }

    private fun noChanges(): Pair<Folder, Event?> {
        return Pair(this, null)
    }

    companion object {
        fun aggId(path: Path): String {
            return "f-" + String(Hex.encodeHex(DigestUtils.sha1(path.toString())))
        }

        fun deserialize(
                revision: Int,
                exists: Boolean,
                aggId: String,
                path: Path
        ): Folder {
            return Folder(
                    revision = revision,
                    exists = exists,
                    aggId = aggId,
                    path = path
            )
        }
    }
}
