package info.maaskant.wmsnotes.model

import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinToString
import java.util.*

sealed class Event(val eventId: Int, val noteId: String, val revision: Int) {
    abstract fun withEventId(eventId: Int): Event

    override fun equals(other: Any?) = kotlinEquals(other = other, properties = arrayOf(Event::eventId, Event::noteId, Event::revision))
    override fun hashCode() = Objects.hash(eventId, noteId, revision)
}

class NoteCreatedEvent(eventId: Int, noteId: String, revision: Int, val title: String) : Event(eventId, noteId, revision) {
    override fun withEventId(eventId: Int): NoteCreatedEvent {
        return NoteCreatedEvent(eventId = eventId, noteId = noteId, revision = revision, title = title)
    }

    override fun toString() = kotlinToString(properties = arrayOf(NoteCreatedEvent::eventId, NoteCreatedEvent::noteId, NoteCreatedEvent::revision, NoteCreatedEvent::title))

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(NoteCreatedEvent::title),
            superEquals = { super.equals(other) })

    override fun hashCode() = Objects.hash(title, super.hashCode())
}

class NoteDeletedEvent(eventId: Int, noteId: String, revision: Int) : Event(eventId, noteId, revision) {
    override fun withEventId(eventId: Int): NoteDeletedEvent {
        return NoteDeletedEvent(eventId = eventId, noteId = noteId, revision = revision)
    }

    override fun toString() = kotlinToString(properties = arrayOf(NoteDeletedEvent::eventId, NoteDeletedEvent::noteId, NoteDeletedEvent::revision))
}

class NoteUndeletedEvent(eventId: Int, noteId: String, revision: Int) : Event(eventId, noteId, revision) {
    override fun withEventId(eventId: Int): NoteUndeletedEvent {
        return NoteUndeletedEvent(eventId = eventId, noteId = noteId, revision = revision)
    }

    override fun toString() = kotlinToString(properties = arrayOf(NoteUndeletedEvent::eventId, NoteUndeletedEvent::noteId, NoteUndeletedEvent::revision))
}

class AttachmentAddedEvent(eventId: Int, noteId: String, revision: Int, val name: String, val content: ByteArray) : Event(eventId, noteId, revision) {
    private val contentLength = content.size

    override fun withEventId(eventId: Int): AttachmentAddedEvent {
        return AttachmentAddedEvent(eventId = eventId, noteId = noteId, revision = revision, name = name, content = content)
    }

    override fun toString() = kotlinToString(properties = arrayOf(AttachmentAddedEvent::eventId, AttachmentAddedEvent::noteId, AttachmentAddedEvent::revision, AttachmentAddedEvent::name, AttachmentAddedEvent::contentLength))

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

class AttachmentDeletedEvent(eventId: Int, noteId: String, revision: Int, val name: String) : Event(eventId, noteId, revision) {
    override fun withEventId(eventId: Int): AttachmentDeletedEvent {
        return AttachmentDeletedEvent(eventId = eventId, noteId = noteId, revision = revision, name = name)
    }

    override fun toString() = kotlinToString(properties = arrayOf(AttachmentDeletedEvent::eventId, AttachmentDeletedEvent::noteId, AttachmentDeletedEvent::revision))

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(AttachmentDeletedEvent::name),
            superEquals = { super.equals(other) })

    override fun hashCode() = Objects.hash(name, super.hashCode())
}

class ContentChangedEvent(eventId: Int, noteId: String, revision: Int, val content: String) : Event(eventId, noteId, revision) {
    private val contentLength = content.length

    override fun withEventId(eventId: Int): ContentChangedEvent {
        return ContentChangedEvent(eventId = eventId, noteId = noteId, revision = revision, content = content)
    }

    override fun toString() = kotlinToString(properties = arrayOf(ContentChangedEvent::eventId, ContentChangedEvent::noteId, ContentChangedEvent::revision, ContentChangedEvent::contentLength))

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(ContentChangedEvent::content),
            superEquals = { super.equals(other) })

    override fun hashCode() = Objects.hash(content, super.hashCode())
}

class TitleChangedEvent(eventId: Int, noteId: String, revision: Int, val title: String) : Event(eventId, noteId, revision) {
    override fun withEventId(eventId: Int): TitleChangedEvent {
        return TitleChangedEvent(eventId = eventId, noteId = noteId, revision = revision, title = title)
    }

    override fun toString() = kotlinToString(properties = arrayOf(TitleChangedEvent::eventId, TitleChangedEvent::noteId, TitleChangedEvent::revision, TitleChangedEvent::title))

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(TitleChangedEvent::title),
            superEquals = { super.equals(other) })

    override fun hashCode() = Objects.hash(title, super.hashCode())
}
