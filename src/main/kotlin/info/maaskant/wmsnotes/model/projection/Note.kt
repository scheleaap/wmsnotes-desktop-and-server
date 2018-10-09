package info.maaskant.wmsnotes.model.projection

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent

class Note {
    var exists = false
        private set

    var noteId = ""
        private set

    var revision: Int = 0
        private set

    var title = ""
        private set

    fun apply(event: Event): Event? {
        if (event.revision != revision + 1) throw IllegalArgumentException("The revision of $event must be ${revision + 1}")
        if (event !is NoteCreatedEvent && event.noteId != noteId) throw IllegalArgumentException("The note id of $event must be $noteId")
        return when (event) {
            is NoteCreatedEvent -> applyCreated(event)
            is NoteDeletedEvent -> applyDeleted(event)
        }
    }

    private fun applyCreated(event: NoteCreatedEvent): Event {
        if (exists) throw IllegalStateException("An existing note cannot be created again ($event)")
        noteId = event.noteId
        revision = event.revision
        title = event.title
        exists = true
        return event
    }

    private fun applyDeleted(event: NoteDeletedEvent): Event? {
        return if (exists) {
            exists = false
            revision = event.revision
            event
        } else {
            null
        }
    }

}
