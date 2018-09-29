package info.maaskant.wmsnotes.model.eventrepository

import info.maaskant.wmsnotes.model.Event
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

interface EventRepository {
    fun getEvent(eventId: Int): Single<Event>
    fun getEvents(afterEventId: Int? = null): Observable<Event>
}

interface AppendableEventRepository : EventRepository {
    // TODO: Add synchronized
    fun appendEvent(event: Event): Completable // Add parameter for previous event id?
}

interface ModifiableEventRepository : EventRepository {
    // TODO: Add synchronized
    fun addEvent(event: Event): Completable
}

