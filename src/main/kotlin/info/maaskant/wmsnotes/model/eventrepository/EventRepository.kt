package info.maaskant.wmsnotes.model.eventrepository

import info.maaskant.wmsnotes.model.Event
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

interface EventRepository {
    fun storeEvent(event: Event): Completable
    fun getEvent(eventId: Int): Single<Event>
    fun getEvents(afterEventId: Int? = null): Observable<Event>
}

