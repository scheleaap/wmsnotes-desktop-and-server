//package info.maaskant.wmsnotes.model.eventstore
//
//import info.maaskant.wmsnotes.desktop.app.database
//import info.maaskant.wmsnotes.desktop.app.logger
//import info.maaskant.wmsnotes.model.Event
//import info.maaskant.wmsnotes.utilities.EventSerializer
//import io.reactivex.Observable
//import io.reactivex.subjects.CompletableSubject
//import io.reactivex.subjects.PublishSubject
//import io.reactivex.subjects.Subject
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class DatabaseEventStore @Inject constructor(private val eventSerializer: EventSerializer) : EventStore {
//    private val logger by logger()
//
//    private val initDone: CompletableSubject = CompletableSubject.create()
//
//    private val newEventSubject: Subject<Event> = PublishSubject.create()
//
//    init {
//        database
//                .update("create table if not exists note_event (event_id integer primary key, type varchar, note_id varchar, data blob) ")
//                .complete()
//                .doOnComplete { logger.debug("Table 'note_event' prepared") }
//                .doOnError { logger.warn("Could not create table 'note_event'", it) }
//                .subscribe(initDone)
//    }
//
//    override fun appendEvent(event: Event) {
////        Thread.sleep(1000)
//        return initDone.concatWith(
//                database
//                        .update("insert into note_event (event_id, type, note_id, data) values (:event_id, :type, :note_id, :data)")
//                        .parameter("event_id", event.eventId)
//                        .parameter("type", event::class.simpleName)
//                        .parameter("note_id", event.noteId)
//                        .parameter("data", eventSerializer.serialize(event))
//                        .complete()
//                        .doOnComplete { logger.debug("Inserted event ${event.eventId}") })
//                .blockingAwait()
//    }
//
//    fun getEvent(eventId: Int): Event? {
////        Thread.sleep(1000)
//        return initDone
//                .toSingle { Unit }
//                .flatMap { _ ->
//                    database
//                            .select("select data from note_event where event_id = :event_id")
//                            .parameter("event_id", eventId)
//                            .get { eventSerializer.deserialize(it.getBytes(1)) }
//                            .singleOrError()
//
//                }
//                .blockingGet()
//    }
//
//    override fun getEvents(afterEventId: Int?): Observable<Event> {
////        Thread.sleep(1000)
//        return initDone
//                .toSingle { Unit }
//                .flatMapObservable { _ ->
//                    if (afterEventId != null) {
//                        database
//                                .select("select data from note_event where event_id > :event_id")
//                                .parameter("event_id", afterEventId)
//                    } else {
//                        database
//                                .select("select data from note_event")
//                    }.get { eventSerializer.deserialize(it.getBytes(1)) }.toObservable()
//                }
//    }
//
//    override fun getEventsOfNote(noteId: String): Observable<Event> {
//        return initDone
//                .toSingle { Unit }
//                .flatMapObservable { _ ->
//                    database
//                            .select("select data from note_event where note_id = :note_id")
//                            .parameter("note_id", noteId)
//                            .get { eventSerializer.deserialize(it.getBytes(1)) }.toObservable()
//                }
//
//    }
//
//    fun getNewEvents(): Observable<Event> {
//        return newEventSubject
//    }
//
//}
