package info.maaskant.wmsnotes.server.event

import info.maaskant.wmsnotes.client.api.GrpcCommandMapper
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.server.command.grpc.Event
import io.mockk.mockk
import io.mockk.verifySequence
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class EventServiceTest {
    private val eventStore: EventStore = mockk(relaxed = true)
    private val grpcEventMapper: GrpcEventMapper = mockk()

    @Test
    fun test() {
        // Given
        val service = EventService(eventStore, grpcEventMapper)
        val request = Event.GetEventsRequest.newBuilder()
                .setAfterEventId(10)
                .build()

        // When
        service.getEvents(request, mockk(relaxed = true))

        // Then
        verifySequence {
            eventStore.getEvents(afterEventId = 10)
        }
    }
}