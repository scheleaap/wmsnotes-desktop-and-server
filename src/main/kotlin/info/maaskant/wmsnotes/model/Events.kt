package info.maaskant.wmsnotes.model

import au.com.console.kassava.kotlinToString
import java.util.*

sealed class Event(val id: String) {
    val eventId = UUID.randomUUID()
}

class NoteCreatedEvent(id: String, val title: String) : Event(id) {
    override fun toString() = kotlinToString(properties = arrayOf(NoteCreatedEvent::id, NoteCreatedEvent::title))

}
class NoteDeletedEvent(id: String) : Event(id) {
    override fun toString() = kotlinToString(properties = arrayOf(NoteDeletedEvent::id))
}
