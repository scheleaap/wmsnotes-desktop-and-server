package info.maaskant.wmsnotes.model.projection

interface NoteProjector {
    fun project(noteId: String, lastRevision: Int): Note
}

class DefaultNoteProjector :NoteProjector {
    override fun project(noteId: String, lastRevision: Int): Note {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}