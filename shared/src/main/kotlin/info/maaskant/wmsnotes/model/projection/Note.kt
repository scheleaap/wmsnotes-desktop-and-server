package info.maaskant.wmsnotes.model.projection

import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinToString
import info.maaskant.wmsnotes.model.*
import org.apache.commons.codec.digest.DigestUtils
import java.util.*

class Note private constructor(
        val revision: Int,
        val exists: Boolean,
        val noteId: String,
        val title: String,
        val attachments: Map<String, ByteArray>,
        val attachmentHashes: Map<String, String>
) {

    private val nameReplacementPattern: Regex = Regex("""[\\\t /&]""")

    constructor() : this(
            revision = 0,
            exists = false,
            noteId = "",
            title = "",
            attachments = emptyMap(),
            attachmentHashes = emptyMap()
    )

    override fun equals(other: Any?) = kotlinEquals(other = other, properties = arrayOf(Note::revision, Note::exists, Note::noteId, Note::title, Note::attachmentHashes))
    override fun hashCode() = Objects.hash(revision, noteId, title, attachmentHashes)
    override fun toString() = kotlinToString(properties = arrayOf(Note::revision, Note::exists, Note::noteId, Note::title, Note::attachmentHashes))

    private fun copy(
            revision: Int = this.revision,
            exists: Boolean = this.exists,
            noteId: String = this.noteId,
            title: String = this.title,
            attachments: Map<String, ByteArray> = this.attachments,
            attachmentHashes: Map<String, String> = this.attachmentHashes
    ): Note {
        return Note(
                revision = revision,
                exists = exists,
                noteId = noteId,
                title = title,
                attachments = attachments,
                attachmentHashes = attachmentHashes
        )
    }


    fun apply(event: Event): Pair<Note, Event?> {
        if (revision == 0) {
            if (event !is NoteCreatedEvent) throw IllegalArgumentException("$event can not be a note's first event")
            if (event.revision != 1) throw IllegalArgumentException("The revision of $event must be ${revision + 1}")
        } else {
            if (event.revision != revision + 1) throw IllegalArgumentException("The revision of $event must be ${revision + 1}")
            if (event.noteId != noteId) throw IllegalArgumentException("The note id of $event must be $noteId")
        }
        return when (event) {
            is NoteCreatedEvent -> applyCreated(event)
            is NoteDeletedEvent -> applyDeleted(event)
            is AttachmentAddedEvent -> applyAttachmentAdded(event)
            is AttachmentDeletedEvent -> applyAttachmentDeleted(event)
        }
    }

    private fun applyAttachmentAdded(event: AttachmentAddedEvent): Pair<Note, Event?> {
        if (!exists) throw IllegalStateException("Not possible if note does not exist ($event)")
        if (event.name.isEmpty()) throw IllegalArgumentException("An attachment name must not be empty ($event)")
        val sanitizedName = event.name.replace(nameReplacementPattern, "_")
        return if (sanitizedName in attachments) {
            if (!Arrays.equals(attachments[sanitizedName], event.content)) throw IllegalStateException("An attachment named $sanitizedName already exists but with different data ($event)")
            noChanges()
        } else {
            copy(
                    revision = event.revision,
                    attachments = attachments + (sanitizedName to event.content),
                    attachmentHashes = attachmentHashes + (sanitizedName to DigestUtils.md5Hex(event.content))
            ) to event
        }
    }

    private fun applyAttachmentDeleted(event: AttachmentDeletedEvent): Pair<Note, Event?> {
        // TODO: Implement test to enable this
        // if (!exists) throw IllegalStateException("Not possible if note does not exist ($event)")
        return if (event.name in attachments) {
            copy(
                    revision = event.revision,
                    attachments = attachments - event.name,
                    attachmentHashes = attachmentHashes - event.name
            ) to event
        } else {
            noChanges()
        }
    }

    private fun applyCreated(event: NoteCreatedEvent): Pair<Note, Event> {
        if (exists) throw IllegalStateException("An existing note cannot be created again ($event)")
        if (event.noteId.isBlank()) throw IllegalArgumentException("Invalid note id $event")
        return copy(
                noteId = event.noteId,
                revision = event.revision,
                title = event.title,
                exists = true
        ) to event
    }

    private fun applyDeleted(event: NoteDeletedEvent): Pair<Note, Event?> {
        return if (exists) {
            copy(
                    exists = false,
                    revision = event.revision
            ) to event
        } else {
            noChanges()
        }
    }

    private fun noChanges(): Pair<Note, Event?> {
        return Pair(this, null)
    }

    companion object {
        fun deserialize(
                revision: Int,
                exists: Boolean,
                noteId: String,
                title: String,
                attachments: Map<String, ByteArray>,
                attachmentHashes: Map<String, String>
        ): Note {
            return Note(
                    revision = revision,
                    exists = exists,
                    noteId = noteId,
                    title = title,
                    attachments = attachments,
                    attachmentHashes = attachmentHashes
            )
        }
    }
}