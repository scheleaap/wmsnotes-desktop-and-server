package info.maaskant.wmsnotes.model.eventstore

import arrow.core.Either
import arrow.core.Either.Companion.left
import arrow.core.Either.Companion.right
import info.maaskant.wmsnotes.model.CommandError
import info.maaskant.wmsnotes.model.CommandError.StorageError
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
    private val newEventSubject: Subject<Event> = PublishSubject.create<Event>().toSerialized()

    override fun getEvents(afterEventId: Int?): Observable<Event> {
        return events
                .values
                .filter { afterEventId == null || it.eventId > afterEventId }
                .toObservable()
                .doOnSubscribe { logger.debug("Loading all events after event id $afterEventId") }
    }

    override fun getEventsOfAggregate(aggId: String, afterRevision: Int?): Observable<Event> {
        return events
                .values
                .filter { it.aggId == aggId }
                .filter { afterRevision == null || it.revision > afterRevision }
                .toObservable()
                .doOnSubscribe { logger.debug("Loading all events of note $aggId") }
    }

    override fun appendEvent(event: Event): Either<StorageError, Event> {
        return if (event.eventId != 0) {
            left(StorageError("Event id must be 0: $event"))
        } else if (event.revision < 0) {
            left(StorageError("Event revision must be greater or equal than 0: $event"))
        } else {
            val lastRevisionOfNote = getLastRevisionOfAggregate(event.aggId)
            if (lastRevisionOfNote == null && event.revision != 1) {
                left(StorageError("Event revision must be 0: $event, $lastRevisionOfNote"))
            } else if (lastRevisionOfNote != null && event.revision != lastRevisionOfNote + 1) {
                left(StorageError("Event revision must be last revision + 1: $event, $lastRevisionOfNote"))
            } else {
                val eventWithId = event.copy(eventId = ++lastEventId)
                events[eventWithId.eventId] = eventWithId
                newEventSubject.onNext(eventWithId)
                right(eventWithId)
            }
        }
    }

    override fun getEventUpdates(): Observable<Event> = newEventSubject

    private fun getLastRevisionOfAggregate(aggId: String): Int? {
        return events
                .values
                .filter { it.aggId == aggId }
                .sortedBy { it.revision }
                .lastOrNull()
                ?.revision
    }

}