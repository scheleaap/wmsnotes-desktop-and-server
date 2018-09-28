package info.maaskant.wmsnotes.model.synchronization

import info.maaskant.wmsnotes.model.eventrepository.EventRepository
import info.maaskant.wmsnotes.model.serialization.EventSerializer
import info.maaskant.wmsnotes.server.command.grpc.Event
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RemoteEventImporterTest {

    private val eventService: EventServiceGrpc.EventServiceBlockingStub = mockk()
    private val eventRepository: EventRepository = mockk()

    @BeforeEach
    fun init() {
        clearMocks(eventService)
    }

    @Test
    fun `no new events`() {
        // Given
        every { eventService.getEvents(any()) }.returns(listOf<Event.GetEventsResponse>().iterator())
        val importer = RemoteEventImporter(eventService, eventRepository)

        // When
        importer.loadAndStoreRemoteEvents()

        // Then
        verify {
            eventRepository.wasNot(Called)
        }
    }

    @Test
    fun `new events`() {
        // Given
        every { eventService.getEvents(any()) }.returns(listOf<Event.GetEventsResponse>(TODO("HIER BEZIG")
        ).iterator())
        val importer = RemoteEventImporter(eventService, eventRepository)

        // When
        importer.loadAndStoreRemoteEvents()

        // Then
        verify {
            eventRepository.wasNot(Called)
        }
    }

}

private fun createResponse