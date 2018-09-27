package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.desktop.app.database
import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.model.serialization.EventSerializer
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.CompletableSubject
import java.util.*

class EventStore(private val eventSerializer: EventSerializer) {

    private val logger by logger()

    private val initDone: CompletableSubject = CompletableSubject.create()

    init {
        database
                .update("create table if not exists note_event (eventId uuid primary key, type varchar, note_id varchar, data blob) ")
                .complete()
                .doOnComplete { logger.debug("Table 'note_event' prepared") }
                .doOnError { logger.warn("Could not create table 'note_event'", it) }
                .subscribe(initDone)
    }

    fun storeEvent(e: Event): Completable {
//        Thread.sleep(1000)
        return initDone.concatWith(
                database
                        .update("insert into note_event (eventId, type, note_id, data) values (:eventId, :type, :note_id, :data)")
                        .parameter("eventId", e.eventId)
                        .parameter("type", e::class.simpleName)
                        .parameter("note_id", e.id)
                        .parameter("data", eventSerializer.serialize(e))
                        .complete()
                        .doOnComplete { logger.debug("Inserted event ${e.eventId}") })
    }

    fun getEvent(eventId: UUID): Single<Event> {
//        Thread.sleep(1000)
        return initDone
                .toSingle { Unit }
                .flatMap { _ ->
                    database
                            .select("select data from note_event where eventId = :eventId")
                            .parameter("eventId", eventId)
                            .get { eventSerializer.deserialize(it.getBytes(1)) }
                            .singleOrError()
                }
    }

    fun getEvents(): Observable<Event> {
//        Thread.sleep(1000)
        return initDone
                .toSingle { Unit }
                .flatMapObservable { _ ->
                    database
                            .select("select data from note_event")
                            .get { eventSerializer.deserialize(it.getBytes(1)) }.toObservable()
                }
    }
}
