package info.maaskant.wmsnotes.model.projection.cache

import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.NoteProjector
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachingNoteProjector @Inject constructor(private val wrapped: NoteProjector, private val noteCache: NoteCache) : NoteProjector {
    override fun project(noteId: String, lastRevision: Int?): Note {
        return wrapped.project(noteId, lastRevision)
    }

    interface NoteCache {
        fun get(noteId: String, revision: Int): Note?
        fun put(note: Note)
    }

}