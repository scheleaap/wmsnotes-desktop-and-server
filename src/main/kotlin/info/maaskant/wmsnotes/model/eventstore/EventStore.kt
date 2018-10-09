package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.model.Event
import io.reactivex.Observable

interface EventStore {
    /**
     * Returns a completing observable that streams all events currently in the repository. Updates are not included.
     */
    fun getEvents(afterEventId: Int? = null): Observable<Event> // Rename to loadEvents?

    /**
     * Returns a completing observable that streams all events applicable to a given note. Updates are not included.
     */
    fun getEventsOfNote(noteId: String): Observable<Event>

    /** Adds a new event to the repository.
     *
     * @param event The event to add. Its event id must be 0.
     * @return A copy of the event with its event id set.
     * */
    fun appendEvent(event: Event): Event

    /**
     * Returns a non-completing observable that streams new events as they are added to the store.
     */
    fun getEventUpdates(): Observable<Event>
}
