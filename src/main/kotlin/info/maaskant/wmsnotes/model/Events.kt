package info.maaskant.wmsnotes.model

import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinToString
import java.util.*

sealed class Event(val eventId: Int, val noteId: String) {
    override fun equals(other: Any?) = kotlinEquals(other = other, properties = arrayOf(Event::eventId, Event::noteId))
    override fun hashCode() = Objects.hash(eventId, noteId)
}

class NoteCreatedEvent(eventId: Int, noteId: String, val title: String) : Event(eventId, noteId) {
    override fun toString() = kotlinToString(properties = arrayOf(NoteCreatedEvent::eventId, NoteCreatedEvent::noteId, NoteCreatedEvent::title))

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(NoteCreatedEvent::title),
            superEquals = { super.equals(other) })

    override fun hashCode() = Objects.hash(title, super.hashCode())
}

class NoteDeletedEvent(eventId: Int, noteId: String) : Event(eventId, noteId) {
    override fun toString() = kotlinToString(properties = arrayOf(NoteDeletedEvent::eventId, NoteDeletedEvent::noteId))
}
