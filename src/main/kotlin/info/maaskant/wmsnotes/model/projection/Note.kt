package info.maaskant.wmsnotes.model.projection

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent

class Note private constructor(
        val revision: Int,
        val exists: Boolean,
        val noteId: String,
        val title: String
) {

    constructor() : this(
            revision = 0,
            exists = false,
            noteId = "",
            title = ""
    )

    private fun copy(
            revision: Int = this.revision,
            exists: Boolean = this.exists,
            noteId: String = this.noteId,
            title: String = this.title
    ): Note {
        return Note(
                revision = revision,
                exists = exists,
                noteId = noteId,
                title = title
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

}
