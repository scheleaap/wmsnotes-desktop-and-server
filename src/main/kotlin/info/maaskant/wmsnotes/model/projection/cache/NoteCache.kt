package info.maaskant.wmsnotes.model.projection.cache

import info.maaskant.wmsnotes.model.projection.Note

interface NoteCache {
    fun get(noteId: String, revision: Int): Note?
    fun getLatest(noteId: String, lastRevision: Int? = null): Note?
    fun put(note: Note)
    fun remove(noteId: String, revision: Int)
}

object NoopNoteCache : NoteCache {
    override fun get(noteId: String, revision: Int): Note? = null
    override fun getLatest(noteId: String, lastRevision: Int?): Note? = null
    override fun put(note: Note) {}
    override fun remove(noteId: String, revision: Int) {}
}
