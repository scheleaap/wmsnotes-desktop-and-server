package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.utilities.logger
import info.maaskant.wmsnotes.model.Event
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Singleton

@Singleton
class InMemoryEventStore : EventStore {
    private val logger by logger()

    private val events: MutableMap<Int, Event> = HashMap()
    private var lastEventId = 0
    private val newEventSubject: Subject<Event> = PublishSubject.create()

    override fun getEvents(afterEventId: Int?): Observable<Event> {
        return events
                .values
                .filter { afterEventId == null || it.eventId > afterEventId }
                .toObservable()
                .doOnSubscribe { logger.debug("Loading all events after event id $afterEventId") }
    }

    override fun getEventsOfNote(noteId: String, afterRevision: Int?): Observable<Event> {
        return events
                .values
                .filter { it.noteId == noteId }
                .filter { afterRevision == null || it.revision > afterRevision }
                .toObservable()
                .doOnSubscribe { logger.debug("Loading all events of note $noteId") }
    }

    override fun appendEvent(event: Event): Event {
        if (event.eventId != 0) throw IllegalArgumentException()
        if (event.eventId in events) throw IllegalArgumentException()
        if (event.revision < 0) throw IllegalArgumentException()
        val lastRevisionOfNote = getLastRevisionOfNote(event.noteId)
        if (lastRevisionOfNote == null && event.revision != 1) {
            throw IllegalArgumentException()
        } else if (lastRevisionOfNote != null && event.revision != lastRevisionOfNote + 1) {
            throw IllegalArgumentException()
        }
        val eventWithId = event.copy(eventId = ++lastEventId)
        events[eventWithId.eventId] = eventWithId
        newEventSubject.onNext(eventWithId)
        return eventWithId
    }

    override fun getEventUpdates(): Observable<Event> = newEventSubject

    private fun getLastRevisionOfNote(noteId: String): Int? {
        return events
                .values
                .filter { it.noteId == noteId }
                .sortedBy { it.revision }
                .lastOrNull()
                ?.revision
    }

}