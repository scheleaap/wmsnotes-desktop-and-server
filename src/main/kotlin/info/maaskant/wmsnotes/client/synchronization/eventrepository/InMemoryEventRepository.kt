package info.maaskant.wmsnotes.client.synchronization.eventrepository

import info.maaskant.wmsnotes.model.Event
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable

class InMemoryEventRepository() : ModifiableEventRepository {

    private val events: MutableMap<Int, Event> = HashMap()

    override fun getEvent(eventId: Int): Event? {
        return if (eventId in events) {
            events[eventId]
        } else {
            null
        }
    }

    override fun getEvents(afterEventId: Int?): Observable<Event> {
        return events.values.filter { afterEventId == null || it.eventId > afterEventId }.toObservable()
    }

    override fun addEvent(event: Event) {
        if (event.eventId in events) {
            throw IllegalArgumentException()
        } else {
            events[event.eventId] = event
        }
    }

    override fun removeEvent(event: Event) {
        if (event.eventId in events) {
            events -= event.eventId
        } else {
            throw IllegalArgumentException()
        }
    }

}
