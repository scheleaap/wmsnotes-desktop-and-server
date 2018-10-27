package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.client.api.GrpcEventMapper
import info.maaskant.wmsnotes.server.command.grpc.Event
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RemoteEventImporterTest {

    private val eventService: EventServiceGrpc.EventServiceBlockingStub = mockk()
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
        val importer = RemoteEventImporter(eventService, eventRepository, grpcEventMapper, InMemoryImporterStateStorage())

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
        val stateStorage = InMemoryImporterStateStorage()

        // When
        val importer1 = RemoteEventImporter(eventService, eventRepository, grpcEventMapper, stateStorage)
        importer1.loadAndStoreRemoteEvents()

        // Given
        every { eventService.getEvents(remoteEventServiceRequest()) }.returns(listOf(event1.first, event2.first).iterator())
        every { eventService.getEvents(remoteEventServiceRequest(1)) }.returns(listOf(event2.first).iterator())

        // When
        val importer2 = RemoteEventImporter(eventService, eventRepository, grpcEventMapper, stateStorage)
        importer2.loadAndStoreRemoteEvents()

        // Then
        verifySequence {
            eventService.getEvents(remoteEventServiceRequest())
            eventRepository.addEvent(event1.second)
            eventService.getEvents(remoteEventServiceRequest(1))
            eventRepository.addEvent(event2.second)
        }
    }

}

private fun remoteEventServiceRequest(afterEventId: Int? = null): Event.GetEventsRequest {
    val builder = Event.GetEventsRequest.newBuilder()
    if (afterEventId != null) {
        builder.afterEventId = afterEventId
    }
    return builder.build()
}

private fun remoteNoteEvent(i: Int): Event.GetEventsResponse {
    val builder = Event.GetEventsResponse.newBuilder().setEventId(i).setNoteId("note-$i").setRevision(i)
    builder.noteCreated = Event.GetEventsResponse.NoteCreatedEvent.newBuilder().setTitle("Title $i").build()
    return builder.build()
}

private fun modelEvent(i: Int): NoteCreatedEvent {
    return NoteCreatedEvent(eventId = i, noteId = "note-$i", revision = i, title = "Title $i")
}
