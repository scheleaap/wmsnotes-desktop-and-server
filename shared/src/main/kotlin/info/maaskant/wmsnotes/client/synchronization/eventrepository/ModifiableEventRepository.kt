package info.maaskant.wmsnotes.client.synchronization.eventrepository

import info.maaskant.wmsnotes.model.Event
import io.reactivex.Observable

interface ModifiableEventRepository {
    /**
     * Returns a single event.
     */
    fun getEvent(eventId: Int): Event?

    /**
     * Returns a completing observable that streams all events currently in the repository. Updates are not included.
     */
    fun getEvents(): Observable<Event>

    fun addEvent(event: Event)

    fun removeEvent(event: Event)
}

