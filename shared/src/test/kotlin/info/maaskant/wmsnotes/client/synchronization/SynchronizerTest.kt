package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.synchronization.eventrepository.InMemoryModifiableEventRepository
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.client.api.GrpcCommandMapper
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import io.mockk.*
import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// TODO:
// resolving conflict (type = LOCAL) if note was deleted remotely
// resolving conflict (type = REMOTE) if note was deleted locally
// resolving conflict (type = BOTH) if note was deleted locally
internal class SynchronizerTest {

    private val localEvents: ModifiableEventRepository = mockk()
    private val remoteEvents: ModifiableEventRepository = mockk()
    private val eventStore: EventStore = mockk()
    private val commandProcessor: CommandProcessor = mockk()
    private val remoteCommandService: CommandServiceGrpc.CommandServiceBlockingStub = mockk()
    private val eventToCommandMapper: EventToCommandMapper = mockk()
    private val grpcCommandMapper: GrpcCommandMapper = mockk()
    private lateinit var stateStorage: InMemorySynchronizerStateStorage
    private val noteProjector: NoteProjector = mockk()

    @BeforeEach
    fun init() {
        clearMocks(
                localEvents,
                remoteEvents,
                eventStore,
                commandProcessor,
                remoteCommandService,
                eventToCommandMapper,
                noteProjector
        )
        every { localEvents.removeEvent(any()) }.just(Runs)
        every { remoteEvents.removeEvent(any()) }.just(Runs)
        stateStorage = InMemorySynchronizerStateStorage()
    }


    @Test
    fun `outbound sync, no remote events, call successful`() {
        // Given
        val event1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val event2 = modelEvent(eventId = 2, noteId = 1, revision = 12)
        val event3 = modelEvent(eventId = 3, noteId = 3, revision = 31)
        every { localEvents.getEvents() }.returns(Observable.just(event1, event2, event3))
        every { remoteEvents.getEvents() }.returns(Observable.empty())
        val remoteRequest1 = givenARemoteResponseForALocalEvent(event1, null, remoteSuccess(event1.eventId, event1.revision))
        val remoteRequest2 = givenARemoteResponseForALocalEvent(event2, event1.revision, remoteSuccess(event2.eventId, event2.revision))
        val remoteRequest3 = givenARemoteResponseForALocalEvent(event3, null, remoteSuccess(event3.eventId, event3.revision))
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteCommandService.postCommand(remoteRequest1)
            localEvents.removeEvent(event1)
            remoteCommandService.postCommand(remoteRequest2)
            localEvents.removeEvent(event2)
            remoteCommandService.postCommand(remoteRequest3)
            localEvents.removeEvent(event3)
        }
    }

    @Test
    fun `outbound sync, no remote events, call fails`() {
        // Given
        val event1 = modelEvent(eventId = -1, noteId = 1, revision = 11)
        val event2 = modelEvent(eventId = -1, noteId = 1, revision = -1)
        val event3 = modelEvent(eventId = 1, noteId = 3, revision = 31)
        every { localEvents.getEvents() }.returns(Observable.just(event1, event2, event3))
        every { remoteEvents.getEvents() }.returns(Observable.empty())
        val remoteRequest1 = givenARemoteResponseForALocalEvent(event1, null, remoteError())
        givenARemoteResponseForALocalEvent(event2, event1.revision, remoteError())
        val remoteRequest3 = givenARemoteResponseForALocalEvent(event3, null, remoteSuccess(event3.eventId, event3.revision))
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteCommandService.postCommand(remoteRequest1)
            remoteCommandService.postCommand(remoteRequest3)
            localEvents.removeEvent(event3)
        }
    }

    @Test
    fun `outbound sync, don't process newly created remote events`() {
        // Given
        val localEvent = modelEvent(eventId = 1, noteId = 1, revision = 1)
        val remoteEventForLocalEvent = modelEvent(eventId = 11, noteId = 1, revision = 11)
        every { localEvents.getEvents() }.returns(Observable.just(localEvent))
        every { remoteEvents.getEvents() }.returns(Observable.empty())
        val remoteRequest = givenARemoteResponseForALocalEvent(localEvent, null, remoteSuccess(remoteEventForLocalEvent.eventId, remoteEventForLocalEvent.revision))
        val s = createSynchronizer()
        s.synchronize()
        every { localEvents.getEvents() }.returns(Observable.empty())
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteEventForLocalEvent))
        givenALocalEventIsReturnedIfARemoteEventIsProcessed(remoteEventForLocalEvent, localEvent.revision, mockk())

        // When
        s.synchronize()

        // Then
        verifySequence {
            remoteCommandService.postCommand(remoteRequest)
        }
        verify {
            remoteEvents.removeEvent(remoteEventForLocalEvent)
        }
        verify(exactly = 0) {
            commandProcessor.blockingProcessCommand(any())
        }
    }

    @Test
    fun `inbound sync, no local events, command successful`() {
        // Given
        val remoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 1)
        val remoteEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 2)
        val remoteEvent3 = modelEvent(eventId = 3, noteId = 3, revision = 1)
        val localEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 11)
        val localEvent2 = modelEvent(eventId = 12, noteId = 1, revision = 12)
        val localEvent3 = modelEvent(eventId = 13, noteId = 3, revision = 31)
        every { localEvents.getEvents() }.returns(Observable.empty())
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteEvent1, remoteEvent2, remoteEvent3))
        val command1 = givenALocalEventIsReturnedIfARemoteEventIsProcessed(remoteEvent1, null, localEvent1)
        val command2 = givenALocalEventIsReturnedIfARemoteEventIsProcessed(remoteEvent2, localEvent1.eventId, localEvent2)
        val command3 = givenALocalEventIsReturnedIfARemoteEventIsProcessed(remoteEvent3, null, localEvent3)
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verifySequence {
            remoteEvents.getEvents()
            commandProcessor.blockingProcessCommand(command1)
            remoteEvents.removeEvent(remoteEvent1)
            commandProcessor.blockingProcessCommand(command2)
            remoteEvents.removeEvent(remoteEvent2)
            commandProcessor.blockingProcessCommand(command3)
            remoteEvents.removeEvent(remoteEvent3)
        }
    }

    @Test
    fun `inbound sync, no local events, command fails`() {
        // Given
        val remoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 1)
        val remoteEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 2)
        val remoteEvent3 = modelEvent(eventId = 3, noteId = 3, revision = 1)
        val localEvent3 = modelEvent(eventId = 13, noteId = 3, revision = 31)
        every { localEvents.getEvents() }.returns(Observable.empty())
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteEvent1, remoteEvent2, remoteEvent3))
        val command1 = givenProcessingOfARemoteEventFails(remoteEvent1)
        givenProcessingOfARemoteEventFails(remoteEvent2)
        val command3 = givenALocalEventIsReturnedIfARemoteEventIsProcessed(remoteEvent3, null, localEvent3)
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verifySequence {
            commandProcessor.blockingProcessCommand(command1)
            commandProcessor.blockingProcessCommand(command3)
        }
    }

    @Test
    fun `inbound sync, don't process newly created local events`() {
        // Given
        val remoteEvent = modelEvent(eventId = 1, noteId = 1, revision = 1)
        val localEventForRemoteEvent = modelEvent(eventId = 11, noteId = 1, revision = 11)
        every { localEvents.getEvents() }.returns(Observable.empty())
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteEvent))
        val command = givenALocalEventIsReturnedIfARemoteEventIsProcessed(remoteEvent, null, localEventForRemoteEvent)
        val s = createSynchronizer()
        s.synchronize()
        every { localEvents.getEvents() }.returns(Observable.just(localEventForRemoteEvent))
        every { remoteEvents.getEvents() }.returns(Observable.empty())
        givenARemoteResponseForALocalEvent(localEventForRemoteEvent, remoteEvent.revision, remoteError())

        // When
        s.synchronize()

        // Then
        verifySequence {
            commandProcessor.blockingProcessCommand(command)
        }
        verify {
            localEvents.removeEvent(localEventForRemoteEvent)
        }
        verify(exactly = 0) {
            remoteCommandService.postCommand(any())
        }
    }

    @Test
    fun `conflicts not processed and published`() {
        // Given
        val localOutboundEvent = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val remoteInboundEvent = modelEvent(eventId = 2, noteId = 1, revision = 11)
        every { localEvents.getEvents() }.returns(Observable.just(localOutboundEvent))
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteInboundEvent))
        val s = createSynchronizer()
        val testObserver = s.getConflicts().test()

        // When
        s.synchronize()

        // Then
        verify {
            remoteCommandService.postCommand(any()).wasNot(Called)
            commandProcessor.blockingProcessCommand(any())?.wasNot(Called)
        }
        testObserver.assertNoErrors()
        assertThat(testObserver.values()).isEqualTo(listOf(setOf(localOutboundEvent.noteId)))
    }

    @Test
    fun `conflict resolution, local`() {
        // Given
        val localOutboundEvent1 = modelEvent(eventId = 10, noteId = 1, revision = 1)
        val localOutboundEvent2 = modelEvent(eventId = 11, noteId = 1, revision = 2)
        val localEvents = createInMemoryEventRepository(localOutboundEvent1, localOutboundEvent2)
        val remoteInboundEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 10)
        val remoteInboundEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 11)
        val remoteEvents = createInMemoryEventRepository(remoteInboundEvent1, remoteInboundEvent2)
        val remoteRequest1 = givenARemoteResponseForALocalEvent(localOutboundEvent1, remoteInboundEvent2.revision, remoteSuccess(remoteInboundEvent2.eventId + 1, remoteInboundEvent2.revision + 1))
        val remoteRequest2 = givenARemoteResponseForALocalEvent(localOutboundEvent2, remoteInboundEvent2.revision + 1, remoteSuccess(remoteInboundEvent2.eventId + 2, remoteInboundEvent2.revision + 2))
        stateStorage.lastLocalRevisions[localOutboundEvent2.noteId] = null
        stateStorage.lastRemoteRevisions[remoteInboundEvent2.noteId] = remoteInboundEvent2.revision - 1
        val s = createSynchronizer(localEvents, remoteEvents)
        s.synchronize()

        // When
        s.resolveConflict(
                noteId = localOutboundEvent2.noteId,
                lastLocalRevision = localOutboundEvent2.revision,
                lastRemoteRevision = remoteInboundEvent2.revision,
                choice = Synchronizer.ConflictResolutionChoice.LOCAL
        )
        s.synchronize()

        // Then
        verifySequence {
            remoteCommandService.postCommand(remoteRequest1)
            remoteCommandService.postCommand(remoteRequest2)
            commandProcessor.blockingProcessCommand(any())?.wasNot(Called)
        }
        assertThat(remoteEvents.getEvent(remoteInboundEvent1.eventId)).isNull()
        assertThat(remoteEvents.getEvent(remoteInboundEvent2.eventId)).isNull()
    }

    @Test
    fun `conflict resolution, remote`() {
        // Given
        val localOutboundEvent1 = modelEvent(eventId = 10, noteId = 1, revision = 1)
        val localOutboundEvent2 = modelEvent(eventId = 11, noteId = 1, revision = 2)
        val localEvents = createInMemoryEventRepository(localOutboundEvent1, localOutboundEvent2)
        val remoteInboundEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 10)
        val remoteInboundEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 11)
        val remoteEvents = createInMemoryEventRepository(remoteInboundEvent1, remoteInboundEvent2)
        val localEventForRemoteInboundEvent1 = modelEvent(eventId = localOutboundEvent2.eventId + 1, noteId = 1, revision = localOutboundEvent2.revision + 1)
        val localEventForRemoteInboundEvent2 = modelEvent(eventId = localOutboundEvent2.eventId + 2, noteId = 1, revision = localOutboundEvent2.revision + 2)
        val command1 = givenALocalEventIsReturnedIfARemoteEventIsProcessed(remoteInboundEvent1, localOutboundEvent2.revision, localEventForRemoteInboundEvent1)
        val command2 = givenALocalEventIsReturnedIfARemoteEventIsProcessed(remoteInboundEvent2, localOutboundEvent2.revision + 1, localEventForRemoteInboundEvent2)
        stateStorage.lastLocalRevisions[localOutboundEvent2.noteId] = null
        stateStorage.lastRemoteRevisions[remoteInboundEvent2.noteId] = remoteInboundEvent2.revision - 1
        val s = createSynchronizer(localEvents, remoteEvents)
        s.synchronize()

        // When
        s.resolveConflict(
                noteId = localOutboundEvent2.noteId,
                lastLocalRevision = localOutboundEvent2.revision,
                lastRemoteRevision = remoteInboundEvent2.revision,
                choice = Synchronizer.ConflictResolutionChoice.REMOTE
        )
        s.synchronize()

        // Then
        verifySequence {
            remoteCommandService.postCommand(any()).wasNot(Called)
            commandProcessor.blockingProcessCommand(command1)
            commandProcessor.blockingProcessCommand(command2)
        }
        assertThat(localEvents.getEvent(localOutboundEvent1.eventId)).isNull()
        assertThat(localEvents.getEvent(localOutboundEvent2.eventId)).isNull()
    }

    @Test
    fun `conflict resolution, both`() {
        // Given
        val localOutboundEvent1 = modelEvent(eventId = 10, noteId = 1, revision = 1)
        val localOutboundEvent2 = modelEvent(eventId = 11, noteId = 1, revision = 2)
        val localEvents = createInMemoryEventRepository(localOutboundEvent1, localOutboundEvent2)
        val remoteInboundEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 10)
        val remoteInboundEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 11)
        val remoteEvents = createInMemoryEventRepository(remoteInboundEvent1, remoteInboundEvent2)

        val localEventForCopiedNote = modelEvent(eventId = localOutboundEvent2.eventId + 1, noteId = 2, revision = 1)
        val localEventForRemoteInboundEvent1 = modelEvent(eventId = localOutboundEvent2.eventId + 2, noteId = 1, revision = localOutboundEvent2.revision + 1)
        val localEventForRemoteInboundEvent2 = modelEvent(eventId = localOutboundEvent2.eventId + 3, noteId = 1, revision = localOutboundEvent2.revision + 2)
        val projectedNoteForLocalOutboundEvent2 = givenAProjection(localOutboundEvent2, createExistingNote())
        val command1 = CreateNoteCommand(null, projectedNoteForLocalOutboundEvent2.title)
        every { commandProcessor.blockingProcessCommand(command1) }.returns(localEventForCopiedNote)
        val command2 = givenALocalEventIsReturnedIfARemoteEventIsProcessed(remoteInboundEvent1, localOutboundEvent2.revision, localEventForRemoteInboundEvent1)
        val command3 = givenALocalEventIsReturnedIfARemoteEventIsProcessed(remoteInboundEvent2, localOutboundEvent2.revision + 1, localEventForRemoteInboundEvent2)
        stateStorage.lastLocalRevisions[localOutboundEvent2.noteId] = localOutboundEvent2.revision
        stateStorage.lastRemoteRevisions[remoteInboundEvent2.noteId] = remoteInboundEvent2.revision
        val s = createSynchronizer(localEvents, remoteEvents)
        s.synchronize()

        // When
        s.resolveConflict(
                noteId = localOutboundEvent2.noteId,
                lastLocalRevision = localOutboundEvent2.revision,
                lastRemoteRevision = remoteInboundEvent2.revision,
                choice = Synchronizer.ConflictResolutionChoice.BOTH
        )
        s.synchronize()

        // Then
        verifySequence {
            remoteCommandService.postCommand(any()).wasNot(Called)
            commandProcessor.blockingProcessCommand(command1)
            commandProcessor.blockingProcessCommand(command2)
            commandProcessor.blockingProcessCommand(command3)
        }
        assertThat(localEvents.getEvent(localOutboundEvent1.eventId)).isNull()
        assertThat(localEvents.getEvent(localOutboundEvent2.eventId)).isNull()
    }

    private fun createInMemoryEventRepository(vararg events: Event): ModifiableEventRepository {
        val r = InMemoryModifiableEventRepository()
        for (event in events) {
            r.addEvent(event)
        }
        return r
    }

    private fun createExistingNote(): Note {
        val note: Note = mockk()
        every { note.exists }.returns(true)
        every { note.title }.returns("projected title")
        return note
    }

    private fun createSynchronizer(
            localEvents: ModifiableEventRepository = this.localEvents,
            remoteEvents: ModifiableEventRepository = this.remoteEvents
    ) =
            Synchronizer(
                    localEvents,
                    remoteEvents,
                    remoteCommandService,
                    eventToCommandMapper,
                    grpcCommandMapper,
                    commandProcessor,
                    noteProjector,
                    stateStorage
            )

    private fun givenALocalEventIsReturnedIfARemoteEventIsProcessed(remoteEvent: Event, lastRevision: Int?, localEvent: Event): Command {
        val command = modelCommand(remoteEvent.noteId, lastRevision)
        every { eventToCommandMapper.map(remoteEvent, lastRevision) }.returns(command)
        every { commandProcessor.blockingProcessCommand(command) }.returns(localEvent)
        return command
    }

    private fun givenProcessingOfARemoteEventFails(remoteEvent: Event): Command {
        val command = modelCommand(remoteEvent.noteId)
        every { eventToCommandMapper.map(remoteEvent, any()) }.returns(command)
        every { commandProcessor.blockingProcessCommand(command) }.throws(IllegalArgumentException())
        return command
    }

    private fun givenAProjection(event: Event, note: Note): Note {
        every { noteProjector.project(event.noteId, event.revision) }.returns(note)
        return note
    }

    private fun givenARemoteResponseForALocalEvent(localEvent: Event, lastRemoteRevision: Int?, postCommandResponse: info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse): info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandRequest {
        val command: Command = mockk()
        every { eventToCommandMapper.map(localEvent, lastRemoteRevision) }.returns(command)
        val remoteRequest: info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandRequest = mockk()
        every { grpcCommandMapper.toGrpcPostCommandRequest(command) }.returns(remoteRequest)
        every { remoteCommandService.postCommand(remoteRequest) }.returns(postCommandResponse)
        return remoteRequest
    }
}

private fun modelCommand(noteId: String, lastRevision: Int? = null): Command {
    return if (lastRevision == null) {
        CreateNoteCommand(noteId, "Title $noteId")
    } else {
        DeleteNoteCommand(noteId, lastRevision)
    }

}

private fun modelEvent(eventId: Int, noteId: Int, revision: Int): NoteCreatedEvent {
    return NoteCreatedEvent(eventId = eventId, noteId = "note-$noteId", revision = revision, title = "Title $noteId")
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
