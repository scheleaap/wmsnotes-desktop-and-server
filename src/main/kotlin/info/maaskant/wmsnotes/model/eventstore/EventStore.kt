package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.model.Event
import io.reactivex.Observable

interface EventStore {
    /**
     * Returns a completing observable that streams all events currently in the repository. Updates are not included.
     */
    fun getEvents(afterEventId: Int? = null): Observable<Event>

    /**
     * Returns a completing observable that streams all events applicable to a given note. Updates are not included.
     */
    fun getEventsOfNote(noteId: String): Observable<Event>

    fun appendEvent(event: Event): Event
}
