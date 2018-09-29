package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.desktop.app.database
import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.model.eventrepository.AppendableEventRepository
import info.maaskant.wmsnotes.model.eventrepository.EventRepository
import info.maaskant.wmsnotes.model.serialization.EventSerializer
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.CompletableSubject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventStore @Inject constructor(private val eventSerializer: EventSerializer) : AppendableEventRepository {

    private val logger by logger()

    private val initDone: CompletableSubject = CompletableSubject.create()

    init {
        database
                .update("create table if not exists note_event (event_id integer primary key, type varchar, note_id varchar, data blob) ")
                .complete()
                .doOnComplete { logger.debug("Table 'note_event' prepared") }
                .doOnError { logger.warn("Could not create table 'note_event'", it) }
                .subscribe(initDone)
    }

    override fun appendEvent(event: Event): Completable {
//        Thread.sleep(1000)
        return initDone.concatWith(
                database
                        .update("insert into note_event (event_id, type, note_id, data) values (:event_id, :type, :note_id, :data)")
                        .parameter("event_id", event.eventId)
                        .parameter("type", event::class.simpleName)
                        .parameter("note_id", event.noteId)
                        .parameter("data", eventSerializer.serialize(event))
                        .complete()
                        .doOnComplete { logger.debug("Inserted event ${event.eventId}") })
    }

    override fun getEvent(eventId: Int): Single<Event> {
//        Thread.sleep(1000)
        return initDone
                .toSingle { Unit }
                .flatMap { _ ->
                    database
                            .select("select data from note_event where event_id = :event_id")
                            .parameter("event_id", eventId)
                            .get { eventSerializer.deserialize(it.getBytes(1)) }
                            .singleOrError()
                }
    }

    override fun getEvents(afterEventId: Int?): Observable<Event> {
//        Thread.sleep(1000)
        return initDone
                .toSingle { Unit }
                .flatMapObservable { _ ->
                    if (afterEventId != null) {
                        database
                                .select("select data from note_event where event_id > :event_id")
                                .parameter("event_id", afterEventId)
                    } else {
                        database
                                .select("select data from note_event")
                    }.get { eventSerializer.deserialize(it.getBytes(1)) }.toObservable()
                }
    }
}
