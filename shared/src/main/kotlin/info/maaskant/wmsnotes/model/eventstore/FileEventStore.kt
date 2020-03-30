package info.maaskant.wmsnotes.model.eventstore

import arrow.core.Either
import arrow.core.Either.Companion.left
import arrow.core.Either.Companion.right
import arrow.core.Some
import arrow.core.nonFatalOrThrow
import info.maaskant.wmsnotes.model.CommandError.StorageError
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.utilities.logger
import info.maaskant.wmsnotes.utilities.serialization.Serializer
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureNanoTime

@Singleton
class FileEventStore @Inject constructor(
        private val rootDirectory: File,
        private val eventSerializer: Serializer<Event>
) : EventStore {

    private val logger by logger()

    private var lastEventId: Int = 0

    private val newEventSubject: Subject<Event> = PublishSubject.create<Event>().toSerialized()

    init {
        logger.debug("Event store directory: $rootDirectory")
        // Replace with persisted value if too slow.
        try {
            val time = measureNanoTime {
                rootDirectory
                        .walkTopDown()
                        .filter { it.isFile && !it.name.startsWith('.') }
                        .map { eventSerializer.deserialize(it.readBytes()) }
                        .forEach {
                            if (it.eventId > lastEventId) {
                                lastEventId = it.eventId
                            }
                        }
            }
            logger.debug("Indexed all events in %.2f seconds".format(Locale.ROOT, time * 0.000000001))
        } catch (e: NoSuchElementException) {
        }
    }

    fun getAggregateIds(): Observable<String> =
            rootDirectory
                    .walkTopDown()
                    .filter { it.isDirectory && it != rootDirectory }
                    .map { it.name }
                    .toObservable()

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

    override fun getEventsOfAggregate(aggId: String, afterRevision: Int?): Observable<Event> {
        return Observable.create { emitter ->
            logger.debug("Loading all events of note $aggId")
            try {
                val afterRevisionFileName: String? = if (afterRevision != null) {
                    "%010d".format(afterRevision)
                } else {
                    null
                }
                noteDirectoryPath(aggId)
                        .walkTopDown()
                        .filter { it.isFile }
                        .sortedBy { it.name }
                        .filter { afterRevisionFileName == null || it.name > afterRevisionFileName }
                        .forEach { emitter.onNext(eventSerializer.deserialize(it.readBytes())) }
                emitter.onComplete()
            } catch (t: Throwable) {
                emitter.onError(t)
            }
        }
    }


    @Synchronized
    override fun appendEvent(event: Event): Either<StorageError, Event> {
        return try {
            if (event.eventId != 0) {
                left(StorageError("Event id must be 0: $event"))
            } else {
                val eventWithId = event.copy(eventId = ++lastEventId)

                val eventFilePath = eventFilePath(eventWithId)
                val previousEventFilePath = eventFilePath(eventWithId.aggId, eventWithId.revision - 1)
                if (eventWithId.revision != 1 && !previousEventFilePath.exists()) {
                    left(StorageError("Previous revision of note ${eventWithId.aggId} does not exist ($previousEventFilePath)"))
                } else if (eventFilePath.exists()) {
                    left(StorageError("Event $eventWithId already exists ($eventFilePath)"))
                } else {
                    logger.debug("Appending event $eventWithId, saving to $eventFilePath")
                    eventFilePath.parentFile.mkdirs()
                    eventFilePath.writeBytes(eventSerializer.serialize(eventWithId))
                    newEventSubject.onNext(eventWithId)
                    right(eventWithId)
                }
            }
        } catch (t: Throwable) {
            left(StorageError("Failed to store event: $event", cause = Some(t.nonFatalOrThrow())))
        }
    }

    override fun getEventUpdates(): Observable<Event> = newEventSubject

    private fun eventFilePath(aggId: String, revision: Int): File = rootDirectory.resolve(aggId).resolve("%010d".format(revision))
    private fun eventFilePath(e: Event): File = eventFilePath(aggId = e.aggId, revision = e.revision)
    private fun noteDirectoryPath(aggId: String) = rootDirectory.resolve(aggId)

}