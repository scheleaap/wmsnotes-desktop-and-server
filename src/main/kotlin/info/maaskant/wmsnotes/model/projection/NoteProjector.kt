package info.maaskant.wmsnotes.model.projection

interface NoteProjector {
    fun project(noteId: String, lastRevision: Int?): Note
}
