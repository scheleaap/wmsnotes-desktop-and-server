package info.maaskant.wmsnotes.utilities.persistence

import info.maaskant.wmsnotes.utilities.serialization.Serializer
import io.reactivex.Scheduler
import java.io.File
import java.util.concurrent.TimeUnit

class FileStateRepository<T>(
        private val serializer: Serializer<T>,
        private val file: File,
        private val scheduler: Scheduler,
        private val timeout: Long,
        private val unit: TimeUnit
) : StateRepository<T> {
    override fun load(): T? =
            if (file.exists()) {
                serializer.deserialize(file.readBytes())
            } else {
                null
            }

    override fun connect(stateProducer: StateProducer<T>) {
        stateProducer.getStateUpdates()
                .subscribeOn(scheduler)
                .apply { if (timeout > 0) debounce(timeout, unit) }
                .subscribe {
                    if (!file.exists()) {
                        file.parentFile.mkdirs()
                    }
                    file.writeBytes(serializer.serialize(it))
                }
    }
}