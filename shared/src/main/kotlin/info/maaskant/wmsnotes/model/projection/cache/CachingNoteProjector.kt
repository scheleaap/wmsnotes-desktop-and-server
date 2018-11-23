package info.maaskant.wmsnotes.model.projection.cache

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.NoteProjector
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachingNoteProjector @Inject constructor(
        private val eventStore: EventStore,
        private val noteCache: NoteCache
) : NoteProjector {

    override fun project(noteId: String, revision: Int): Note {
        val cached: Note? = noteCache.getLatest(noteId, lastRevision = revision)
        return eventStore
                .getEventsOfNote(noteId, afterRevision = cached?.revision)
                .filter { it.revision <= revision }
                .reduceWith({ cached ?: Note() }, { note: Note, event: Event -> note.apply(event).component1() })
                .blockingGet()
    }

    override fun projectAndUpdate(noteId: String): Observable<Note> {
        return Observable.defer {
            val cached: Note? = noteCache.getLatest(noteId, lastRevision = null)
            val current: Note = eventStore
                    .getEventsOfNote(noteId, afterRevision = cached?.revision)
                    .reduceWith({ cached ?: Note() }, { note: Note, event: Event -> note.apply(event).component1() })
                    .blockingGet()
            eventStore
                    .getEventsOfNoteWithUpdates(noteId, afterRevision = current.revision)
                    .scan(current) { note: Note, event: Event -> note.apply(event).component1() }
                    .doOnNext { noteCache.put(it) }
        }
    }
}