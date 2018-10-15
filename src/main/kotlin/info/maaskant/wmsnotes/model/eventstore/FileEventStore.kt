package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.utilities.serialization.EventSerializer
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureNanoTime

@Singleton
class FileEventStore @Inject constructor(private val rootDirectory: File, private val eventSerializer: EventSerializer) : EventStore {

    private val logger by logger()

    private var lastEventId: Int = 0

    private val newEventSubject: Subject<Event> = PublishSubject.create()

    init {
        // Replace with persisted value if too slow.
        try {
            val time = measureNanoTime {
                rootDirectory
                        .walkTopDown()
                        .filter { it.isFile }
                        .map { eventSerializer.deserialize(it.readBytes()) }
                        .forEach {
                            if (it.eventId > lastEventId) {
                                lastEventId = it.eventId
                            }
                        }
            }
            logger.debug("Indexed all events in %.1f seconds".format(Locale.ROOT, time/1000000.0))
        } catch (e: NoSuchElementException) {
        }
    }

    override fun getEvents(afterEventId: Int?): Observable<Event> {
        // TODO Use caching instead of reading all files
        return Observable.create { emitter ->
            try {
                logger.debug("Loading all events after event id $afterEventId")
                rootDirectory
                        .walkTopDown()
                        .filter { it.isFile }
                        .map { eventSerializer.deserialize(it.readBytes()) }
                        .filter { (afterEventId == null || it.eventId > afterEventId) }
                        .sortedBy { it.eventId }
                        .forEach { emitter.onNext(it) }
                emitter.onComplete()
            } catch (t: Throwable) {
                emitter.onError(t)
            }
        }
    }

    override fun getEventsOfNote(noteId: String): Observable<Event> {
        return Observable.create { emitter ->
            logger.debug("Loading all events of note $noteId")
            try {
                noteDirectoryPath(noteId)
                        .walkTopDown()
                        .filter { it.isFile }
                        .forEach { emitter.onNext(eventSerializer.deserialize(it.readBytes())) }
                emitter.onComplete()
            } catch (t: Throwable) {
                emitter.onError(t)
            }
        }
    }

    @Synchronized
    override fun appendEvent(event: Event): Event {
        if (event.eventId != 0) throw IllegalArgumentException("Expected id of event $event to be 0")
        val eventWithId = event.withEventId(++lastEventId)

        val eventFilePath = eventFilePath(eventWithId)
        if (eventWithId.revision != 1) {
            val previousEventFilePath = eventFilePath(eventWithId.noteId, eventWithId.revision - 1)
            if (!previousEventFilePath.exists()) throw IllegalArgumentException("Previous revision of note ${eventWithId.noteId} does not exist ($previousEventFilePath)")
        }
        if (eventFilePath.exists()) throw IllegalArgumentException("Event $eventWithId already exists ($eventFilePath)")

        logger.debug("Appending event $eventWithId, saving to $eventFilePath")
        eventFilePath.parentFile.mkdirs()
        eventFilePath.writeBytes(eventSerializer.serialize(eventWithId))
        newEventSubject.onNext(eventWithId)
        return eventWithId
    }

    override fun getEventUpdates(): Observable<Event> = newEventSubject

    private fun eventFilePath(noteId: String, revision: Int): File = rootDirectory.resolve(noteId).resolve("%010d".format(revision))
    private fun eventFilePath(e: Event): File = eventFilePath(noteId = e.noteId, revision = e.revision)
    private fun noteDirectoryPath(noteId: String) = rootDirectory.resolve(noteId)

}