package info.maaskant.wmsnotes.model.projection

import io.reactivex.Observable

interface NoteProjector {
    fun project(noteId: String, revision: Int): Note
    fun projectAndUpdate(noteId: String): Observable<Note>
}
