package info.maaskant.wmsnotes.model

import au.com.console.kassava.kotlinToString
import java.util.*

sealed class Event(val eventId: UUID, val id: String)

class NoteCreatedEvent(id: String, val title: String, eventId: UUID = UUID.randomUUID()) : Event(eventId, id) {
    override fun toString() = kotlinToString(properties = arrayOf(NoteCreatedEvent::eventId, NoteCreatedEvent::id, NoteCreatedEvent::title))
}

class NoteDeletedEvent(id: String, eventId: UUID = UUID.randomUUID()) : Event(eventId, id) {
    override fun toString() = kotlinToString(properties = arrayOf(NoteDeletedEvent::eventId, NoteDeletedEvent::id))
}
