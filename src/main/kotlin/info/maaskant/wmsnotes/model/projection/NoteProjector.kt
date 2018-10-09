package info.maaskant.wmsnotes.model.projection

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.eventstore.EventStore
import javax.inject.Inject
import javax.inject.Singleton

interface NoteProjector {
    fun project(noteId: String, lastRevision: Int?): Note
}

@Singleton
class DefaultNoteProjector @Inject constructor(private val eventStore: EventStore) : NoteProjector {
    override fun project(noteId: String, lastRevision: Int?): Note {
        return eventStore
                .getEventsOfNote(noteId)
                .filter { lastRevision == null || it.revision <= lastRevision }
                .reduceWith({ Note() }, { note: Note, event: Event -> note.apply(event).component1() })
                .blockingGet()
    }
}