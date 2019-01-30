package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.api.GrpcCommandMapper
import info.maaskant.wmsnotes.client.synchronization.CompensatingActionExecutor.EventIdAndRevision
import info.maaskant.wmsnotes.client.synchronization.CompensatingActionExecutor.ExecutionResult
import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Tests written while traveling, code to be implemented next")
internal class CompensatingActionExecutorTest {
    private val commandProcessor: CommandProcessor = mockk()
    private val remoteCommandService: CommandServiceGrpc.CommandServiceBlockingStub = mockk()
    private val eventToCommandMapper: EventToCommandMapper = mockk()
    private val grpcCommandMapper: GrpcCommandMapper = mockk()

    @BeforeEach
    fun init() {
        clearMocks(
                commandProcessor,
                remoteCommandService,
                eventToCommandMapper,
                grpcCommandMapper
        )
    }

    @Test
    fun test() {
        TODO("Investigate whether we can move lastKnown*Revisions from Synchronizer to CAE")
    }

    @Test
    fun `local and remote events, successful`() {
        // Given
        val localEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val localEvent2 = modelEvent(eventId = 12, noteId = 2, revision = 2)
        val remoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val remoteEvent2 = modelEvent(eventId = 2, noteId = 2, revision = 12)
        val compensatingAction = CompensatingAction(
                compensatedLocalEvents = emptyList(),
                compensatedRemoteEvents = emptyList(),
                newLocalEvents = listOf(localEvent1, localEvent2),
                newRemoteEvents = listOf(remoteEvent1, remoteEvent2)
        )
        val localCommand1 = givenAnEventIsReturnedIfAnotherEventIsProcessedLocally(localEvent1, null, localEvent1)
        val localCommand2 = givenAnEventIsReturnedIfAnotherEventIsProcessedLocally(localEvent2, localEvent1.eventId, localEvent2)
        val remoteRequest1 = givenARemoteResponseForAnEvent(remoteEvent1, null, remoteSuccess(remoteEvent1.eventId, remoteEvent1.revision))
        val remoteRequest2 = givenARemoteResponseForAnEvent(remoteEvent2, remoteEvent1.revision, remoteSuccess(remoteEvent2.eventId, remoteEvent2.revision))
        val executor = createExecutor()

        // When
        val result = executor.execute(compensatingAction)

        // Then
        assertThat(result).isEqualTo(ExecutionResult(
                success = true,
                newLocalEvents = listOf(EventIdAndRevision(localEvent1), EventIdAndRevision(localEvent2)),
                newRemoteEvents = listOf(EventIdAndRevision(remoteEvent1), EventIdAndRevision(remoteEvent2))
        ))
        verifySequence {
            remoteCommandService.postCommand(remoteRequest1)
            remoteCommandService.postCommand(remoteRequest2)
            commandProcessor.blockingProcessCommand(localCommand1)
            commandProcessor.blockingProcessCommand(localCommand2)
        }
    }

    @Test
    fun `local and remote events, remote fails`() {
        // Given
        val localEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val localEvent2 = modelEvent(eventId = 12, noteId = 2, revision = 2)
        val remoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val remoteEvent2 = modelEvent(eventId = 2, noteId = 2, revision = 12)
        val compensatingAction = CompensatingAction(
                compensatedLocalEvents = emptyList(),
                compensatedRemoteEvents = emptyList(),
                newLocalEvents = listOf(localEvent1, localEvent2),
                newRemoteEvents = listOf(remoteEvent1, remoteEvent2)
        )
        givenAnEventIsReturnedIfAnotherEventIsProcessedLocally(localEvent1, null, localEvent1)
        givenAnEventIsReturnedIfAnotherEventIsProcessedLocally(localEvent2, localEvent1.eventId, localEvent2)
        val remoteRequest1 = givenARemoteResponseForAnEvent(remoteEvent1, null, remoteError())
        val remoteRequest2 = givenARemoteResponseForAnEvent(remoteEvent2, remoteEvent1.revision, remoteSuccess(remoteEvent2.eventId, remoteEvent2.revision))
        val executor = createExecutor()

        // When
        val result = executor.execute(compensatingAction)

        // Then
        assertThat(result).isEqualTo(ExecutionResult(
                success = true,
                newLocalEvents = emptyList(),
                newRemoteEvents = emptyList()
        ))
        verify {
            remoteCommandService.postCommand(remoteRequest1)
            remoteCommandService.postCommand(remoteRequest2).wasNot(Called)
            commandProcessor.blockingProcessCommand(any())?.wasNot(Called)
        }
    }

    @Test
    fun `local and remote events, local fails`() {
        // Given
        val localEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val localEvent2 = modelEvent(eventId = 12, noteId = 2, revision = 2)
        val remoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val remoteEvent2 = modelEvent(eventId = 2, noteId = 2, revision = 12)
        val compensatingAction = CompensatingAction(
                compensatedLocalEvents = emptyList(),
                compensatedRemoteEvents = emptyList(),
                newLocalEvents = listOf(localEvent1, localEvent2),
                newRemoteEvents = listOf(remoteEvent1, remoteEvent2)
        )
        val localCommand1 = givenProcessingOfARemoteEventFails(localEvent1)
        val localCommand2 = givenAnEventIsReturnedIfAnotherEventIsProcessedLocally(localEvent2, localEvent1.eventId, localEvent2)
        val remoteRequest1 = givenARemoteResponseForAnEvent(remoteEvent1, null, remoteSuccess(remoteEvent1.eventId, remoteEvent1.revision))
        val remoteRequest2 = givenARemoteResponseForAnEvent(remoteEvent2, remoteEvent1.revision, remoteSuccess(remoteEvent2.eventId, remoteEvent2.revision))
        val executor = createExecutor()

        // When
        val result = executor.execute(compensatingAction)

        // Then
        assertThat(result).isEqualTo(ExecutionResult(
                success = true,
                newLocalEvents = listOf(EventIdAndRevision(localEvent1), EventIdAndRevision(localEvent2)),
                newRemoteEvents = listOf(EventIdAndRevision(remoteEvent1), EventIdAndRevision(remoteEvent2))
        ))
        verifySequence {
            remoteCommandService.postCommand(remoteRequest1)
            remoteCommandService.postCommand(remoteRequest2)
            commandProcessor.blockingProcessCommand(localCommand1)
        }
        verify {
            commandProcessor.blockingProcessCommand(localCommand2)?.wasNot(Called)
        }
    }

    private fun createExecutor() =
            CompensatingActionExecutor(
                    remoteCommandService,
                    null,
                    eventToCommandMapper,
                    grpcCommandMapper,
                    commandProcessor
            )

    private fun givenAnEventIsReturnedIfAnotherEventIsProcessedLocally(inputEvent: Event, lastRevision: Int?, outputEvent: Event): Command {
        val command = modelCommand(inputEvent.noteId, lastRevision)
        every { eventToCommandMapper.map(inputEvent, lastRevision) }.returns(command)
        every { commandProcessor.blockingProcessCommand(command) }.returns(outputEvent)
        return command
    }

    private fun givenProcessingOfARemoteEventFails(remoteEvent: Event): Command {
        val command = modelCommand(remoteEvent.noteId)
        every { eventToCommandMapper.map(remoteEvent, any()) }.returns(command)
        every { commandProcessor.blockingProcessCommand(command) }.throws(IllegalArgumentException())
        return command
    }

    private fun givenARemoteResponseForAnEvent(event: Event, lastRemoteRevision: Int?, postCommandResponse: info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse): info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandRequest {
        val command: Command = mockk()
        every { eventToCommandMapper.map(event, lastRemoteRevision) }.returns(command)
        val remoteRequest: info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandRequest = mockk()
        every { grpcCommandMapper.toGrpcPostCommandRequest(command) }.returns(remoteRequest)
        every { remoteCommandService.postCommand(remoteRequest) }.returns(postCommandResponse)
        return remoteRequest
    }

    companion object {
        internal fun modelCommand(noteId: String, lastRevision: Int? = null): Command {
            return if (lastRevision == null) {
                CreateNoteCommand(noteId, "Title $noteId")
            } else {
                DeleteNoteCommand(noteId, lastRevision)
            }

        }

        internal fun modelEvent(eventId: Int, noteId: Int, revision: Int): NoteCreatedEvent {
            return NoteCreatedEvent(eventId = eventId, noteId = "note-$noteId", revision = revision, title = "Title $noteId")
        }

        internal fun remoteSuccess(newEventId: Int, newRevision: Int): info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse {
            return info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse
                    .newBuilder()
                    .setStatus(info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse.Status.SUCCESS)
                    .setNewEventId(newEventId)
                    .setNewRevision(newRevision)
                    .build()
        }

        internal fun remoteError(): info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse {
            return info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse
                    .newBuilder()
                    .setStatus(info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse.Status.INTERNAL_ERROR).build()
        }
    }
}
