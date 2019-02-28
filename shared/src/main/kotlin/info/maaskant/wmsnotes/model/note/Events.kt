package info.maaskant.wmsnotes.model.note

import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinToString
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import java.util.*

sealed class NoteEvent(eventId: Int, aggId: String, revision: Int) : Event(eventId, aggId, revision)

class NoteCreatedEvent(eventId: Int, aggId: String, revision: Int, val path: Path, val title: String, val content: String) : NoteEvent(eventId, aggId, revision) {
    private val contentLength = content.length

    override fun copy(eventId: Int, revision: Int): NoteCreatedEvent =
            NoteCreatedEvent(
                    eventId = eventId,
                    aggId = aggId,
                    revision = revision,
                    path = path,
                    title = title,
                    content = content
            )

    override fun toString() = kotlinToString(properties = arrayOf(
            NoteCreatedEvent::eventId,
            NoteCreatedEvent::aggId,
            NoteCreatedEvent::revision,
            NoteCreatedEvent::path,
            NoteCreatedEvent::title,
            NoteCreatedEvent::contentLength
    ))

    override fun canEqual(other: Any?) = other is NoteCreatedEvent

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(NoteCreatedEvent::path, NoteCreatedEvent::title, NoteCreatedEvent::content),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(path, title, content, super.hashCode())
}

class NoteDeletedEvent(eventId: Int, aggId: String, revision: Int) : NoteEvent(eventId, aggId, revision) {
    override fun copy(eventId: Int, revision: Int): NoteDeletedEvent =
            NoteDeletedEvent(eventId = eventId, aggId = aggId, revision = revision)

    override fun toString() = kotlinToString(properties = arrayOf(NoteDeletedEvent::eventId, NoteDeletedEvent::aggId, NoteDeletedEvent::revision))

    override fun canEqual(other: Any?) = other is NoteDeletedEvent
}

class NoteUndeletedEvent(eventId: Int, aggId: String, revision: Int) : NoteEvent(eventId, aggId, revision) {
    override fun copy(eventId: Int, revision: Int): NoteUndeletedEvent =
            NoteUndeletedEvent(eventId = eventId, aggId = aggId, revision = revision)

    override fun toString() = kotlinToString(properties = arrayOf(NoteUndeletedEvent::eventId, NoteUndeletedEvent::aggId, NoteUndeletedEvent::revision))

    override fun canEqual(other: Any?) = other is NoteUndeletedEvent
}

class AttachmentAddedEvent(eventId: Int, aggId: String, revision: Int, val name: String, val content: ByteArray) : NoteEvent(eventId, aggId, revision) {
    private val contentLength = content.size

    override fun copy(eventId: Int, revision: Int): AttachmentAddedEvent =
            AttachmentAddedEvent(eventId = eventId, aggId = aggId, revision = revision, name = name, content = content)

    override fun toString() = kotlinToString(properties = arrayOf(AttachmentAddedEvent::eventId, AttachmentAddedEvent::aggId, AttachmentAddedEvent::revision, AttachmentAddedEvent::name, AttachmentAddedEvent::contentLength))

    override fun canEqual(other: Any?) = other is AttachmentAddedEvent

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as AttachmentAddedEvent

        if (name != other.name) return false
        if (!Arrays.equals(content, other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + Arrays.hashCode(content)
        return result
    }
}

class AttachmentDeletedEvent(eventId: Int, aggId: String, revision: Int, val name: String) : NoteEvent(eventId, aggId, revision) {
    override fun copy(eventId: Int, revision: Int): AttachmentDeletedEvent =
            AttachmentDeletedEvent(eventId = eventId, aggId = aggId, revision = revision, name = name)

    override fun toString() = kotlinToString(properties = arrayOf(AttachmentDeletedEvent::eventId, AttachmentDeletedEvent::aggId, AttachmentDeletedEvent::revision))

    override fun canEqual(other: Any?) = other is AttachmentDeletedEvent

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(AttachmentDeletedEvent::name),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(name, super.hashCode())
}

class ContentChangedEvent(eventId: Int, aggId: String, revision: Int, val content: String) : NoteEvent(eventId, aggId, revision) {
    private val contentLength = content.length

    override fun copy(eventId: Int, revision: Int): ContentChangedEvent =
            ContentChangedEvent(eventId = eventId, aggId = aggId, revision = revision, content = content)

    override fun toString() = kotlinToString(properties = arrayOf(ContentChangedEvent::eventId, ContentChangedEvent::aggId, ContentChangedEvent::revision, ContentChangedEvent::contentLength))

    override fun canEqual(other: Any?) = other is ContentChangedEvent

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(ContentChangedEvent::content),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(content, super.hashCode())
}

class TitleChangedEvent(eventId: Int, aggId: String, revision: Int, val title: String) : NoteEvent(eventId, aggId, revision) {
    override fun copy(eventId: Int, revision: Int): TitleChangedEvent =
            TitleChangedEvent(eventId = eventId, aggId = aggId, revision = revision, title = title)

    override fun toString() = kotlinToString(properties = arrayOf(TitleChangedEvent::eventId, TitleChangedEvent::aggId, TitleChangedEvent::revision, TitleChangedEvent::title))

    override fun canEqual(other: Any?) = other is TitleChangedEvent

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(TitleChangedEvent::title),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(title, super.hashCode())
}

class MovedEvent(eventId: Int, aggId: String, revision: Int, val path: Path) : NoteEvent(eventId, aggId, revision) {
    override fun copy(eventId: Int, revision: Int): MovedEvent =
            MovedEvent(eventId = eventId, aggId = aggId, revision = revision, path = path)

    override fun toString() = kotlinToString(properties = arrayOf(MovedEvent::eventId, MovedEvent::aggId, MovedEvent::revision, MovedEvent::path))

    override fun canEqual(other: Any?) = other is MovedEvent

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(MovedEvent::path),
            superEquals = { super.equals(other) })

    override fun hashCode() = Objects.hash(path, super.hashCode())
}

