package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.model.Event
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable

class InMemoryEventStore : EventStore {

    private val events: MutableMap<Int, Event> = HashMap()
    private var lastEventId = 0

    override fun getEvents(afterEventId: Int?): Observable<Event> {
        return events
                .values
                .filter { afterEventId == null || it.eventId > afterEventId }
                .toObservable()
    }

    override fun getEventsOfNote(noteId: String): Observable<Event> {
        return events
                .values
                .filter { it.noteId == noteId }
                .toObservable()
    }

    override fun appendEvent(event: Event) {
        if (event.eventId != 0) throw IllegalArgumentException()
        if (event.eventId in events) throw IllegalArgumentException()
        if (event.revision < 0) throw IllegalArgumentException()
        val lastRevisionOfNote = getLastRevisionOfNote(event.noteId)
        if (lastRevisionOfNote == null && event.revision != 1) {
            throw IllegalArgumentException()
        } else if (lastRevisionOfNote != null && event.revision != lastRevisionOfNote + 1) {
            throw IllegalArgumentException()
        }
        val eventWithId = event.withEventId(++lastEventId)
        events[eventWithId.eventId] = eventWithId
    }

    private fun getLastRevisionOfNote(noteId: String): Int? {
        return events
                .values
                .filter { it.noteId == noteId }
                .sortedBy { it.revision }
                .lastOrNull()
                ?.revision
    }

}