package info.maaskant.wmsnotes.model.synchronization

import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.model.EventStore
import info.maaskant.wmsnotes.model.Model
import info.maaskant.wmsnotes.server.api.GrpcConverters
import info.maaskant.wmsnotes.server.command.grpc.Event
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class InboundSynchronizer internal constructor(
        private val channel: ManagedChannel,
        private val eventStore: EventStore,
        private val model: Model
) {

    private val logger by logger()

    private var started: Boolean = false

    private val blockingStub: EventServiceGrpc.EventServiceBlockingStub = EventServiceGrpc.newBlockingStub(channel)

    constructor(host: String, port: Int, eventStore: EventStore, model: Model) :
            this(ManagedChannelBuilder.forAddress(host, port)
                    // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                    // needing certificates.
                    .usePlaintext()
                    .build(),
                    eventStore,
                    model)

    fun start() {
        if (!started) {
            Observable
                    .interval(0, 10, TimeUnit.SECONDS)
                    .observeOn(Schedulers.io())
                    .subscribe {
                        getAndProcessRemoteEvents()
                    }
            started = true
        }
    }

    @Throws(InterruptedException::class)
    fun stop() {
        if (started) {
            channel.shutdown().awaitTermination(60, TimeUnit.SECONDS)
            started = false
        }
    }

    private fun getAndProcessRemoteEvents() {
        logger.info("Getting and processing remote events")
        val request = Event.GetEventsRequest.newBuilder().build()
        try {
            val response: Iterator<Event.GetEventsResponse> =
                    blockingStub.getEvents(request)
            response.forEachRemaining {
                val event = GrpcConverters.toModelClass(it)
                logger.info("$event")
                this.eventStore.storeEvent(event)
                this.model.events.onNext(event)
            }
        } catch (e: StatusRuntimeException) {
            logger.warn("Error while retrieving events: ${e.status.code}")
            return
        }
    }

}