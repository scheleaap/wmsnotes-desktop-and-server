package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.client.api.GrpcEventMapper
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.server.command.grpc.Event
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import io.grpc.Deadline
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RemoteEventImporterTest {

    private val eventService: EventServiceGrpc.EventServiceBlockingStub = mockk()
    private val grpcDeadline: Deadline? = null
    private val eventRepository: ModifiableEventRepository = mockk()
    private val grpcEventMapper: GrpcEventMapper = GrpcEventMapper()

    @BeforeEach
    fun init() {
        clearMocks(eventService, eventRepository)
        every { eventRepository.addEvent(any()) }.answers {}
    }

    @Test
    fun `load and store events`() {
        // Given
        val event1 = remoteNoteEvent(i = 1) to modelEvent(i = 1)
        val event2 = remoteNoteEvent(i = 2) to modelEvent(i = 2)
        every { eventService.getEvents(any()) }.returns(listOf(event1.first, event2.first).iterator())
        val importer = RemoteEventImporter(eventService, grpcDeadline, eventRepository, grpcEventMapper, EventImporterState(null))

        // When
        importer.loadAndStoreRemoteEvents()

        // Then
        verifySequence {
            eventService.getEvents(remoteEventServiceRequest())
            eventRepository.addEvent(event1.second)
            eventRepository.addEvent(event2.second)
        }
    }

    @Test
    fun `only load new events`() {
        // Given
        val event1 = remoteNoteEvent(i = 1) to modelEvent(i = 1)
        val event2 = remoteNoteEvent(i = 2) to modelEvent(i = 2)
        every { eventService.getEvents(remoteEventServiceRequest()) }.returns(listOf(event1.first).iterator())
        every { eventService.getEvents(remoteEventServiceRequest(1)) }.returns(emptyList<Event.GetEventsResponse>().iterator())

        // When
        val importer1 = RemoteEventImporter(eventService, grpcDeadline, eventRepository, grpcEventMapper, EventImporterState(null))
        val stateObserver = importer1.getStateUpdates().test()
        importer1.loadAndStoreRemoteEvents()

        // Given
        every { eventService.getEvents(remoteEventServiceRequest()) }.returns(listOf(event1.first, event2.first).iterator())
        every { eventService.getEvents(remoteEventServiceRequest(1)) }.returns(listOf(event2.first).iterator())

        // When
        val importer2 = RemoteEventImporter(eventService, grpcDeadline, eventRepository, grpcEventMapper, stateObserver.values().last())
        importer2.loadAndStoreRemoteEvents()

        // Then
        verifySequence {
            eventService.getEvents(remoteEventServiceRequest())
            eventRepository.addEvent(event1.second)
            eventService.getEvents(remoteEventServiceRequest(1))
            eventRepository.addEvent(event2.second)
        }
    }

    companion object {
        internal fun remoteEventServiceRequest(afterEventId: Int? = null): Event.GetEventsRequest {
            val builder = Event.GetEventsRequest.newBuilder()
            if (afterEventId != null) {
                builder.afterEventId = afterEventId
            }
            return builder.build()
        }

        internal fun remoteNoteEvent(i: Int): Event.GetEventsResponse {
            val builder = Event.GetEventsResponse.newBuilder().setEventId(i).setNoteId("note-$i").setRevision(i)
            builder.noteCreated = Event.GetEventsResponse.NoteCreatedEvent.newBuilder()
                    .setPath(Path("path-$i").toString())
                    .setTitle("Title $i")
                    .setContent("Text $i")
                    .build()
            return builder.build()
        }

        internal fun modelEvent(i: Int): NoteCreatedEvent {
            return NoteCreatedEvent(eventId = i, noteId = "note-$i", revision = i, path = Path("path-$i"), title = "Title $i", content = "Text $i")
        }
    }
}