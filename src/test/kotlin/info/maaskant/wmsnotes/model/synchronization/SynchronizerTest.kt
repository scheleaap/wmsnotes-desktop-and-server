package info.maaskant.wmsnotes.model.synchronization

import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.server.api.GrpcConverters
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import io.mockk.*
import io.reactivex.Observable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SynchronizerTest {

    private val localEvents: ModifiableEventRepository = mockk()
    private val remoteEvents: ModifiableEventRepository = mockk()
    private val eventStore: EventStore = mockk()
    private val commandProcessor: CommandProcessor = mockk()
    private val remoteCommandService: CommandServiceGrpc.CommandServiceBlockingStub = mockk()
    private val remoteEventToLocalCommandMapper: RemoteEventToLocalCommandMapper = mockk()

    @BeforeEach
    fun init() {
        clearMocks(
                localEvents,
                remoteEvents,
                eventStore,
                commandProcessor,
                remoteCommandService,
                remoteEventToLocalCommandMapper
        )
    }


    @Test
    fun `outbound sync, no remote events, call successful`() {
        // Given
        val event1 = modelEvent(1)
        val event2 = modelEvent(1)
        val event3 = modelEvent(3)
        every { localEvents.getCurrentEvents() }.returns(Observable.just(event1, event2, event3))
        every { localEvents.removeEvent(any()) }.answers {}
        every { remoteEvents.getCurrentEvents() }.returns(Observable.empty())
        every { remoteCommandService.postCommand(remoteRequest(event1, null)) }.returns(remoteSuccess(1))
        every { remoteCommandService.postCommand(remoteRequest(event2, 1)) }.returns(remoteSuccess(2))
        every { remoteCommandService.postCommand(remoteRequest(event3, null)) }.returns(remoteSuccess(1))
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getCurrentEvents(any())
            remoteCommandService.postCommand(remoteRequest(event1, null))
            localEvents.removeEvent(event1)
            remoteCommandService.postCommand(remoteRequest(event2, 1))
            localEvents.removeEvent(event2)
            remoteCommandService.postCommand(remoteRequest(event3, null))
            localEvents.removeEvent(event3)
        }
    }

    @Test
    fun `outbound sync, no remote events, call fails`() {
        // Given
        val event1 = modelEvent(1)
        val event2 = modelEvent(2)
        val event3 = modelEvent(1)
        every { localEvents.getCurrentEvents() }.returns(Observable.just(event1, event2))
        every { localEvents.removeEvent(any()) }.answers {}
        every { remoteEvents.getCurrentEvents() }.returns(Observable.empty())
        every { remoteCommandService.postCommand(remoteRequest(event1, null)) }.returns(remoteError())
        every { remoteCommandService.postCommand(remoteRequest(event2, null)) }.returns(remoteSuccess(1))
        every { remoteCommandService.postCommand(remoteRequest(event3, 1)) }.returns(remoteError())
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getCurrentEvents(any())
            remoteCommandService.postCommand(remoteRequest(event1, null))
            remoteCommandService.postCommand(remoteRequest(event2, null))
            localEvents.removeEvent(event2)
        }
    }

    @Test
    fun `inbound sync, no local events, command successful`() {
        // Given
        val remoteEvent1 = modelEvent(1)
        val remoteEvent2 = modelEvent(1)
        val remoteEvent3 = modelEvent(3)
        val command1 = modelCommand(1)
        val command2 = modelCommand(2)
        val command3 = modelCommand(3)
        val localEvent1 = modelEvent(1)
        val localEvent2 = modelEvent(1)
        val localEvent3 = modelEvent(3)
        every { localEvents.getCurrentEvents() }.returns(Observable.empty())
        every { remoteEvents.getCurrentEvents() }.returns(Observable.just(remoteEvent1, remoteEvent2, remoteEvent3))
        every { remoteEventToLocalCommandMapper.map(remoteEvent1) }.returns(command1)
        every { remoteEventToLocalCommandMapper.map(remoteEvent2) }.returns(command2)
        every { remoteEventToLocalCommandMapper.map(remoteEvent3) }.returns(command3)
        every { commandProcessor.blockingProcessCommand(command1, null) }.returns(localEvent1)
        every { commandProcessor.blockingProcessCommand(command2, 1) }.returns(localEvent2)
        every { commandProcessor.blockingProcessCommand(command3, null) }.returns(localEvent3)
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verifySequence {
            commandProcessor.blockingProcessCommand(command1, null)
            commandProcessor.blockingProcessCommand(command2, 1)
            commandProcessor.blockingProcessCommand(command3, null)
        }
    }

    private fun createSynchronizer() =
            Synchronizer(
                    localEvents,
                    remoteEvents,
                    remoteCommandService,
                    remoteEventToLocalCommandMapper,
                    commandProcessor
            )
}

private fun modelCommand(id: Int): Command {
    return CreateNoteCommand("note-$id", "Title $id")
}

private fun modelEvent(id: Int): NoteCreatedEvent {
    return NoteCreatedEvent(id, "note-$id", "Title $id")
}

private fun remoteRequest(event: Event, lastEventId: Int?): info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandRequest {
    return GrpcConverters.toGrpcPostCommandRequest(event, lastEventId)
}

private fun remoteSuccess(newEventId: Int?): info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse {
    val builder = info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse
            .newBuilder()
            .setStatus(info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse.Status.SUCCESS)
    if (newEventId != null) {
        builder.setNewEventId(newEventId)
    }
    return builder.build()
}

private fun remoteError(): info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse {
    return info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse
            .newBuilder()
            .setStatus(info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse.Status.INTERNAL_ERROR).build()
}