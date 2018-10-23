package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.model.Event
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DelayingEventStore @Inject constructor(private val wrapped: EventStore) : EventStore {
    override fun getEvents(afterEventId: Int?): Observable<Event> {
        return wrapped
                .getEvents(afterEventId)
                .delaySubscription(2, TimeUnit.SECONDS)
    }

    override fun getEventsOfNote(noteId: String, afterRevision: Int?): Observable<Event> {
        return wrapped
                .getEventsOfNote(noteId, afterRevision)
                .concatMap { Observable.just(it).delay(500, TimeUnit.MILLISECONDS) }
    }

    override fun appendEvent(event: Event): Event {
        return wrapped.appendEvent(event)
    }

    override fun getEventUpdates(): Observable<Event> {
        return wrapped.getEventUpdates()
    }
}
