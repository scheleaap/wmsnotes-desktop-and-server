package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.model.Event
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable

class InMemoryEventStore : EventStore {
    private val events: MutableMap<Int, Event> = HashMap()

    override fun getEvents(afterEventId: Int?): Observable<Event> {
        return events.values.filter { afterEventId == null || it.eventId > afterEventId }.toObservable()
    }

    override fun getEventsOfNote(noteId: String): Observable<Event> {
        return events
                .values
                .filter { it.noteId == noteId }
                .toObservable()
    }

    override fun appendEvent(event: Event) {
        if (event.eventId in events) {
            throw IllegalArgumentException()
        } else {
            events[event.eventId] = event
        }
    }

}