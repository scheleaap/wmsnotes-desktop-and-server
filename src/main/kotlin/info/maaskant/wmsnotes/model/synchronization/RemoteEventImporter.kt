package info.maaskant.wmsnotes.model.synchronization

import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.model.eventrepository.EventRepository
import info.maaskant.wmsnotes.server.api.GrpcConverters
import info.maaskant.wmsnotes.server.command.grpc.Event
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import io.grpc.StatusRuntimeException
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteEventImporter @Inject constructor(
        private val eventService: EventServiceGrpc.EventServiceBlockingStub,
        private val eventRepository: EventRepository
) {

    private val logger by logger()

    private var started: Boolean = false
    private var timerDisposable: Disposable? = null

    fun loadAndStoreRemoteEvents() {
        logger.info("Getting and processing remote events")
//        val request = Event.GetEventsRequest.newBuilder().build()
//        try {
//            val response: Iterator<Event.GetEventsResponse> =
//                    eventService.getEvents(request)
//            response.forEachRemaining {
//                val event = GrpcConverters.toModelClass(it)
//                logger.info("$event")
//                eventRepository.storeEvent(event)
//            }
//        } catch (e: StatusRuntimeException) {
//            logger.warn("Error while retrieving events: ${e.status.code}")
//            return
//        }
    }

}