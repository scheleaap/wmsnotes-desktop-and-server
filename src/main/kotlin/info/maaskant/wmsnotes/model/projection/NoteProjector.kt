package info.maaskant.wmsnotes.model.projection

import info.maaskant.wmsnotes.model.eventstore.EventStore
import javax.inject.Inject

interface NoteProjector {
    fun project(noteId: String, lastRevision: Int): Note
}

class DefaultNoteProjector @Inject constructor(/*private val eventStore: EventStore*/) : NoteProjector {

    override fun project(noteId: String, lastRevision: Int): Note {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}