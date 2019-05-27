package info.maaskant.wmsnotes.model.note

import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinToString
import info.maaskant.wmsnotes.model.Aggregate
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.util.*

class Note private constructor(
        override val revision: Int,
        val exists: Boolean,
        override val aggId: String,
        val path: Path,
        val title: String,
        val content: String,
        val attachments: Map<String, ByteArray>,
        val attachmentHashes: Map<String, String>
) : Aggregate<Note> {

    private val contentLength: Int = content.length

    constructor() : this(
            revision = 0,
            exists = false,
            aggId = "",
            title = "",
            path = Path(),
            content = "",
            attachments = emptyMap(),
            attachmentHashes = emptyMap()
    )

    override fun equals(other: Any?) = kotlinEquals(other = other, properties = arrayOf(Note::aggId, Note::revision, Note::exists, Note::path, Note::title, Note::content, Note::attachmentHashes))
    fun equalsIgnoringRevision(other: Any?) = kotlinEquals(other = other, properties = arrayOf(Note::aggId, Note::exists, Note::path, Note::title, Note::content, Note::attachmentHashes))
    override fun hashCode() = Objects.hash(aggId, revision, exists, path, title, content, attachmentHashes)
    override fun toString() = kotlinToString(properties = arrayOf(Note::aggId, Note::revision, Note::exists, Note::path, Note::title, Note::contentLength, Note::attachmentHashes))

    private fun copy(
            revision: Int = this.revision,
            exists: Boolean = this.exists,
            aggId: String = this.aggId,
            path: Path = this.path,
            title: String = this.title,
            content: String = this.content,
            attachments: Map<String, ByteArray> = this.attachments,
            attachmentHashes: Map<String, String> = this.attachmentHashes
    ): Note {
        return Note(
                revision = revision,
                exists = exists,
                aggId = aggId,
                path = path,
                title = title,
                content = content,
                attachments = attachments,
                attachmentHashes = attachmentHashes
        )
    }

    override fun apply(event: Event): Pair<Note, Event?> {
        val expectedRevision = revision + 1
        if (revision == 0) {
            if (event !is NoteCreatedEvent) throw IllegalArgumentException("$event cannot be a note's first event")
            if (event.revision != expectedRevision) throw IllegalArgumentException("The revision of $event must be $expectedRevision")
        } else {
            if (event.revision != expectedRevision) throw IllegalArgumentException("The revision of $event must be $expectedRevision")
            if (event.aggId != aggId) throw IllegalArgumentException("The aggregate id of $event must be $aggId")
        }
        return when (event) {
            is NoteEvent -> when (event) {
                is NoteCreatedEvent -> applyCreated(event)
                is NoteDeletedEvent -> applyDeleted(event)
                is NoteUndeletedEvent -> applyUndeleted(event)
                is AttachmentAddedEvent -> applyAttachmentAdded(event)
                is AttachmentDeletedEvent -> applyAttachmentDeleted(event)
                is ContentChangedEvent -> applyContentChanged(event)
                is TitleChangedEvent -> applyTitleChanged(event)
                is MovedEvent -> applyMoved(event)
            }
            else -> throw IllegalArgumentException("Wrong event type $event")
        }
    }

    private fun applyAttachmentAdded(event: AttachmentAddedEvent): Pair<Note, NoteEvent?> {
        if (event.name.isEmpty()) throw IllegalArgumentException("An attachment name must not be empty ($event)")
        val sanitizedName = event.name.replace(nameReplacementPattern, "_")
        return if (sanitizedName in attachments) {
            if (!Arrays.equals(attachments[sanitizedName], event.content)) throw IllegalStateException("An attachment named $sanitizedName already exists but with different data ($event)")
            noChanges()
        } else {
            copy(
                    revision = event.revision,
                    attachments = attachments + (sanitizedName to event.content),
                    attachmentHashes = attachmentHashes + (sanitizedName to hash(event.content))
            ) to event
        }
    }

    private fun applyAttachmentDeleted(event: AttachmentDeletedEvent): Pair<Note, NoteEvent?> {
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

    private fun applyContentChanged(event: ContentChangedEvent): Pair<Note, NoteEvent?> {
        return if (content == event.content) noChanges() else copy(
                revision = event.revision,
                content = event.content
        ) to event
    }

    private fun applyCreated(event: NoteCreatedEvent): Pair<Note, NoteEvent> {
        if (exists) throw IllegalStateException("An existing note cannot be created again ($event)")
        if (event.aggId.isBlank()
                || !event.aggId.startsWith("n-")
                || !event.aggId.matches(aggIdPattern)
        ) throw IllegalArgumentException("Invalid aggregate id $event")
        return copy(
                aggId = event.aggId,
                revision = event.revision,
                path = event.path,
                title = event.title,
                content = event.content,
                exists = true
        ) to event
    }

    private fun applyDeleted(event: NoteDeletedEvent): Pair<Note, NoteEvent?> {
        return if (exists) {
            copy(
                    exists = false,
                    revision = event.revision
            ) to event
        } else {
            noChanges()
        }
    }

    private fun applyMoved(event: MovedEvent): Pair<Note, NoteEvent?> {
        return if (path == event.path) noChanges() else copy(
                revision = event.revision,
                path = event.path
        ) to event
    }

    private fun applyTitleChanged(event: TitleChangedEvent): Pair<Note, NoteEvent?> {
        return if (title == event.title) noChanges() else copy(
                revision = event.revision,
                title = event.title
        ) to event
    }

    private fun applyUndeleted(event: NoteUndeletedEvent): Pair<Note, NoteEvent?> {
        return if (!exists) {
            copy(
                    exists = true,
                    revision = event.revision
            ) to event
        } else {
            noChanges()
        }
    }

    private fun noChanges(): Pair<Note, NoteEvent?> {
        return Pair(this, null)
    }

    companion object {
        private val aggIdPattern = Regex("""^n-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$""")
        private val nameReplacementPattern = Regex("""[\\\t /&]""")

        fun deserialize(
                revision: Int,
                exists: Boolean,
                aggId: String,
                path: Path,
                title: String,
                content: String,
                attachments: Map<String, ByteArray>,
                attachmentHashes: Map<String, String>
        ): Note {
            return Note(
                    revision = revision,
                    exists = exists,
                    aggId = aggId,
                    path = path,
                    title = title,
                    content = content,
                    attachments = attachments,
                    attachmentHashes = attachmentHashes
            )
        }

        private fun hash(content: ByteArray): String {
            // The following does not work on Android:
            // return DigestUtils.md5Hex(content)
            // Source: https://stackoverflow.com/a/9284092
            return String(Hex.encodeHex(DigestUtils.md5(content)))
        }

        fun randomAggId(): String =
                "n-" + UUID.randomUUID().toString()
    }
}
