package info.maaskant.wmsnotes.model.projection

import info.maaskant.wmsnotes.model.Event
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable

interface NoteProjector {
    fun project(noteId: String, revision: Int): Note
    fun projectAndUpdate(noteId: String): Observable<Note>

    companion object {
        fun project(base: Note, events: List<Event>): Note =
                events.toObservable()
                        .reduceWith({ base }, { note: Note, event: Event -> note.apply(event).component1() })
                        .blockingGet()
    }
}
