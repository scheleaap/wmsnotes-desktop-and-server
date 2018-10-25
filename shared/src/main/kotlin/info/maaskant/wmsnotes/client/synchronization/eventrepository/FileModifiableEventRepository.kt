package info.maaskant.wmsnotes.client.synchronization.eventrepository

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.utilities.serialization.EventSerializer
import io.reactivex.Observable
import java.io.File
import javax.inject.Inject

class FileModifiableEventRepository @Inject constructor(private val directory: File, private val eventSerializer: EventSerializer) : ModifiableEventRepository {

    override fun getEvent(eventId: Int): Event? {
        val eventPath = eventPath(eventId)
        return if (eventPath.exists()) {
            eventSerializer.deserialize(eventPath.readBytes())
        } else {
            null
        }
    }

    override fun getEvents(): Observable<Event> {
        return Observable.create { emitter ->
            try {
                directory
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
    override fun addEvent(event: Event) {
        val eventPath = eventPath(event)
        if (eventPath.exists()) throw IllegalArgumentException("Event ${event.eventId} already exists ($eventPath)")
        eventPath.parentFile.mkdirs()
        eventPath.writeBytes(eventSerializer.serialize(event))
    }

    @Synchronized
    override fun removeEvent(event: Event) {
        val eventPath = eventPath(event)
        if (!eventPath.exists()) throw IllegalArgumentException("Event ${event.eventId} does not exist ($eventPath)")
        eventPath.delete()
    }

    private fun eventPath(eventId: Int): File = directory.resolve("%010d".format(eventId))
    private fun eventPath(e: Event): File = eventPath(e.eventId)

}