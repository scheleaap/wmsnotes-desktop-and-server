package info.maaskant.wmsnotes.model.eventrepository

import info.maaskant.wmsnotes.model.Event
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

interface EventRepository {
    /**
     * Returns a single event.
     */
    fun getEvent(eventId: Int): Event?

    /**
     * Returns a completing observable that streams all events currently in the repository. Updates are not included.
     */
    fun getCurrentEvents(afterEventId: Int? = null): Observable<Event>

}

interface AppendableEventRepository : EventRepository {
    // TODO: Add synchronized
    fun appendEvent(event: Event) // Add parameter for previous event id?
}

interface ModifiableEventRepository : EventRepository {
    fun addEvent(event: Event)
    fun removeEvent(event: Event)
}

