package info.maaskant.wmsnotes.utilities.persistence

import info.maaskant.wmsnotes.utilities.logger
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
    private val logger by logger()

    override fun load(): T? =
            if (file.exists()) {
                serializer.deserialize(file.readBytes())
            } else {
                null
            }

    override fun connect(stateProducer: StateProducer<T>) {
        stateProducer.getStateUpdates()
                .subscribeOn(scheduler)
                .let { if (timeout > 0) it.throttleLast(timeout, unit) else it }
                .subscribe {
                    if (!file.exists()) {
                        file.parentFile.mkdirs()
                    }
                    val data: ByteArray = serializer.serialize(it)
                    file.writeBytes(data)
                    logger.debug("Wrote {} bytes to {}", data.size, file.path)
                }
    }
}