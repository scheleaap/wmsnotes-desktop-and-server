package info.maaskant.wmsnotes.model.synchronization

import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.server.api.GrpcConverters
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import io.mockk.*
import io.reactivex.Observable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// TODO
// - ignore inbound event of previously created outbound event
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
        val event1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val event2 = modelEvent(eventId = 2, noteId = 1, revision = 12)
        val event3 = modelEvent(eventId = 3, noteId = 3, revision = 31)
        every { localEvents.getCurrentEvents() }.returns(Observable.just(event1, event2, event3))
        every { localEvents.removeEvent(any()) }.answers {}
        every { remoteEvents.getCurrentEvents() }.returns(Observable.empty())
        every { remoteCommandService.postCommand(remoteRequest(event1, null)) }.returns(remoteSuccess(event1.eventId, event1.revision))
        every { remoteCommandService.postCommand(remoteRequest(event2, event1.revision)) }.returns(remoteSuccess(event2.eventId, event2.revision))
        every { remoteCommandService.postCommand(remoteRequest(event3, null)) }.returns(remoteSuccess(event3.eventId, event3.revision))
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getCurrentEvents(any())
            remoteCommandService.postCommand(remoteRequest(event1, null))
            localEvents.removeEvent(event1)
            remoteCommandService.postCommand(remoteRequest(event2, event1.revision))
            localEvents.removeEvent(event2)
            remoteCommandService.postCommand(remoteRequest(event3, null))
            localEvents.removeEvent(event3)
        }
    }

    @Test
    fun `outbound sync, no remote events, call fails`() {
        // Given
        val event1 = modelEvent(eventId = -1, noteId = 1, revision = 11)
        val event2 = modelEvent(eventId = -1, noteId = 1, revision = -1)
        val event3 = modelEvent(eventId = 1, noteId = 3, revision = 31)
        every { localEvents.getCurrentEvents() }.returns(Observable.just(event1, event2, event3))
        every { localEvents.removeEvent(any()) }.answers {}
        every { remoteEvents.getCurrentEvents() }.returns(Observable.empty())
        every { remoteCommandService.postCommand(remoteRequest(event1, null)) }.returns(remoteError())
        every { remoteCommandService.postCommand(remoteRequest(event2, event1.revision)) }.returns(remoteError())
        every { remoteCommandService.postCommand(remoteRequest(event3, null)) }.returns(remoteSuccess(event3.eventId, event3.revision))
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getCurrentEvents(any())
            remoteCommandService.postCommand(remoteRequest(event1, null))
            remoteCommandService.postCommand(remoteRequest(event3, null))
            localEvents.removeEvent(event3)
        }
    }

    @Test
    fun `inbound sync, no local events, command successful`() {
        // Given
        val remoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 1)
        val remoteEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 2)
        val remoteEvent3 = modelEvent(eventId = 3, noteId = 3, revision = 1)
        val command1 = modelCommand(1)
        val command2 = modelCommand(2)
        val command3 = modelCommand(3)
        val localEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 11)
        val localEvent2 = modelEvent(eventId = 12, noteId = 1, revision = 12)
        val localEvent3 = modelEvent(eventId = 13, noteId = 3, revision = 31)
        every { localEvents.getCurrentEvents() }.returns(Observable.empty())
        every { remoteEvents.getCurrentEvents() }.returns(Observable.just(remoteEvent1, remoteEvent2, remoteEvent3))
        every { remoteEventToLocalCommandMapper.map(remoteEvent1) }.returns(command1)
        every { remoteEventToLocalCommandMapper.map(remoteEvent2) }.returns(command2)
        every { remoteEventToLocalCommandMapper.map(remoteEvent3) }.returns(command3)
        every { commandProcessor.blockingProcessCommand(command1, null) }.returns(localEvent1)
        every { commandProcessor.blockingProcessCommand(command2, localEvent1.eventId) }.returns(localEvent2)
        every { commandProcessor.blockingProcessCommand(command3, null) }.returns(localEvent3)
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verifySequence {
            commandProcessor.blockingProcessCommand(command1, null)
            commandProcessor.blockingProcessCommand(command2, localEvent1.revision)
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

private fun modelCommand(noteId: Int): Command {
    return CreateNoteCommand("note-$noteId", "Title $noteId")
}

private fun modelEvent(eventId: Int, noteId: Int, revision: Int): NoteCreatedEvent {
    return NoteCreatedEvent(eventId = eventId, noteId = "note-$noteId", revision = revision, title = "Title $noteId")
}

private fun remoteRequest(event: Event, lastRevision: Int?): info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandRequest {
    return GrpcConverters.toGrpcPostCommandRequest(event, lastRevision)
}

private fun remoteSuccess(newEventId: Int, newRevision: Int): info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse {
    return info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse
            .newBuilder()
            .setStatus(info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse.Status.SUCCESS)
            .setNewEventId(newEventId)
            .setNewRevision(newRevision)
            .build()
}

private fun remoteError(): info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse {
    return info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse
            .newBuilder()
            .setStatus(info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse.Status.INTERNAL_ERROR).build()
}
