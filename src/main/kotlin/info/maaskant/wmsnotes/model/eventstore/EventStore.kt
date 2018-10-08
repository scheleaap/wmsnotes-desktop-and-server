package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.model.Event
import io.reactivex.Observable

interface EventStore {
    /**
     * Returns a single event.
     */
    fun getEvent(eventId: Int): Event?

    /**
     * Returns a completing observable that streams all events currently in the repository. Updates are not included.
     */
    fun getEvents(afterEventId: Int? = null): Observable<Event>

    fun appendEvent(event: Event) // TODO: Add parameter for previous revision
}
