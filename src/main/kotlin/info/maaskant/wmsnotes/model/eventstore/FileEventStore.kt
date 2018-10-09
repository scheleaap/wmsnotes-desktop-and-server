package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.utilities.serialization.EventSerializer
import io.reactivex.Observable
import java.io.File
import javax.inject.Inject

class FileEventStore @Inject constructor(private val rootDirectory: File, private val eventSerializer: EventSerializer) : EventStore {

    private var lastEventId: Int = 0

    init {
        // Replace with persisted value if too slow.
        try {
            rootDirectory
                    .walkTopDown()
                    .filter { it.isFile }
                    .map { eventSerializer.deserialize(it.readBytes()) }
                    .forEach {
                        if (it.eventId > lastEventId) {
                            lastEventId = it.eventId
                        }
                    }
        } catch (e: NoSuchElementException) {
        }
    }

    override fun getEvents(afterEventId: Int?): Observable<Event> {
        // TODO Use caching instead of reading all files
        return Observable.create { emitter ->
            try {
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
        if (eventFilePath.exists()) throw IllegalArgumentException("Event ${eventWithId} already exists ($eventFilePath)")
        eventFilePath.parentFile.mkdirs()
        eventFilePath.writeBytes(eventSerializer.serialize(eventWithId))
        return eventWithId
    }

    private fun eventFilePath(noteId: String, revision: Int): File = rootDirectory.resolve(noteId).resolve("%010d".format(revision))
    private fun eventFilePath(e: Event): File = eventFilePath(noteId = e.noteId, revision = e.revision)
    private fun noteDirectoryPath(noteId: String) = rootDirectory.resolve(noteId)

}