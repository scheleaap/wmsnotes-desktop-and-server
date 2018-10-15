package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.model.Event
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DelayingEventStore @Inject constructor(private val wrapped: EventStore) : EventStore {

    override fun getEvents(afterEventId: Int?): Observable<Event> {
        return wrapped
                .getEvents(afterEventId)
                .doOnSubscribe { Thread.sleep(2000) }
    }

    override fun getEventsOfNote(noteId: String): Observable<Event> {
        return wrapped
                .getEventsOfNote(noteId)
                .doOnNext { Thread.sleep(1000) }
    }

    override fun appendEvent(event: Event): Event {
        return wrapped.appendEvent(event)
    }

    override fun getEventUpdates(): Observable<Event> {
        return wrapped.getEventUpdates()
    }

}