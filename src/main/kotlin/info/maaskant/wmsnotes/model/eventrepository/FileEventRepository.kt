package info.maaskant.wmsnotes.model.eventrepository

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.serialization.EventSerializer
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File
import javax.inject.Inject

class FileEventRepository @Inject constructor(private val directory: File, private val eventSerializer: EventSerializer) : EventRepository {

    override fun storeEvent(event: Event): Completable {
        return Completable.create {
            try {
                directory.mkdirs()
                eventPath(event).writeBytes(eventSerializer.serialize(event))
                it.onComplete()
            } catch (t: Throwable) {
                it.onError(t)
            }
        }
    }

    override fun getEvent(eventId: Int): Single<Event> {
        return Single.create { emitter ->
            try {
                emitter.onSuccess(eventSerializer.deserialize(eventPath(eventId).readBytes()))
            } catch (t: Throwable) {
                emitter.onError(t)
            }
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

    private fun eventPath(eventId: Int): File = directory.resolve("%010d".format(eventId))
    private fun eventPath(e: Event): File = eventPath(e.eventId)

}