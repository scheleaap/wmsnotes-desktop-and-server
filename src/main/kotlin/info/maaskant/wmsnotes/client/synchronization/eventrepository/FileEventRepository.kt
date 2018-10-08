package info.maaskant.wmsnotes.client.synchronization.eventrepository

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.serialization.EventSerializer
import io.reactivex.Observable
import java.io.File
import javax.inject.Inject

class FileEventRepository @Inject constructor(private val directory: File, private val eventSerializer: EventSerializer) : ModifiableEventRepository {

    override fun getEvent(eventId: Int): Event? {
        val eventPath = eventPath(eventId)
        return if (eventPath.exists()) {
            eventSerializer.deserialize(eventPath.readBytes())
        } else {
            null
        }
    }

    override fun getEvents(afterEventId: Int?): Observable<Event> {
        return Observable.create { emitter ->
            try {
                val afterFileName: String? = "%010d".format(afterEventId)
                directory
                        .walkTopDown()
                        .filter { it.isFile && (afterFileName == null || it.name > afterFileName) }
                        .forEach { emitter.onNext(eventSerializer.deserialize(it.readBytes())) }
                emitter.onComplete()
            } catch (t: Throwable) {
                emitter.onError(t)
            }
        }
    }

    @Synchronized
    override fun addEvent(event: Event) {
        val eventPath = eventPath(event)
        if (eventPath.exists()) throw IllegalStateException("Event ${event.eventId} already exists ($eventPath)")
        directory.mkdirs()
        eventPath.writeBytes(eventSerializer.serialize(event))
    }

    @Synchronized
    override fun removeEvent(event: Event) {
        val eventPath = eventPath(event)
        if (!eventPath.exists()) throw IllegalStateException("Event ${event.eventId} does not exist ($eventPath)")
        eventPath.delete()
    }

    private fun eventPath(eventId: Int): File = directory.resolve("%010d".format(eventId))
    private fun eventPath(e: Event): File = eventPath(e.eventId)

}