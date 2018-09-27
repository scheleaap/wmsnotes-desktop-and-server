package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.desktop.app.database
import info.maaskant.wmsnotes.desktop.app.logger
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.CompletableSubject
import org.davidmoten.rx.jdbc.Database
import java.io.ByteArrayOutputStream
import java.util.*

class EventStore(private val kryo: Kryo) {

    private val logger by logger()

    private val initDone: CompletableSubject = CompletableSubject.create()

    init {
        kryo.register(NoteCreatedEvent::class.java)
        kryo.register(NoteDeletedEvent::class.java)
        kryo.register(UUID::class.java)

        database
                .update("create table if not exists note_event (eventId uuid primary key, type varchar, note_id varchar, data blob) ")
                .complete()
                .doOnComplete { logger.debug("Table 'note_event' prepared") }
                .doOnError { logger.warn("Could not create table 'note_event'", it) }
                .subscribe(initDone)
    }

    fun storeEvent(e: Event): Completable {
//        Thread.sleep(1000)
        val byteArrayOutputStream = ByteArrayOutputStream()
        byteArrayOutputStream.use { baos ->
            Output(baos).use { ko -> kryo.writeClassAndObject(ko, e) }
        }
        val data: ByteArray = byteArrayOutputStream.toByteArray()

        return initDone.concatWith(
                database
                        .update("insert into note_event (eventId, type, note_id, data) values (:eventId, :type, :note_id, :data)")
                        .parameter("eventId", e.eventId)
                        .parameter("type", e::class.simpleName)
                        .parameter("note_id", e.id)
                        .parameter("data", data.inputStream())
                        .complete()
                        .doOnComplete { logger.debug("Inserted event ${e.eventId}") })
    }

    fun getEvent(eventId: UUID): Single<Event> {
        return initDone
                .toSingle { Unit }
                .flatMap { _ ->
                    database
                            .select("select type, data from note_event where eventId = :eventId")
                            .parameter("eventId", eventId)
                            .get {
                                when (it.getString(1)) {
                                    NoteCreatedEvent::class.simpleName -> {
                                        val bytes: ByteArray = it.getBytes(2)
                                        val kryoInput = Input(bytes)
                                        kryoInput.use { ki ->
                                            kryo.readObject(ki, NoteCreatedEvent::class.java) as Event
                                        }
                                    }
                                    else -> throw IllegalArgumentException()
                                }
                            }.singleOrError()
                }
    }
}
