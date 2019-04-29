package info.maaskant.wmsnotes.client.synchronization.commandexecutor

import info.maaskant.wmsnotes.client.api.GrpcCommandMapper
import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.note.CreateNoteCommand
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import io.grpc.Deadline
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

@Suppress("SameParameterValue")
internal class RemoteCommandExecutorTest {
    private val grpcCommandService: CommandServiceGrpc.CommandServiceBlockingStub = mockk()
    private val grpcCommandMapper: GrpcCommandMapper = mockk()
    private val grpcDeadline = Deadline.after(1, TimeUnit.SECONDS)

    @BeforeEach
    fun init() {
        clearMocks(
                grpcCommandService,
                grpcCommandMapper
        )
        every { grpcCommandService.withDeadline(any()) }.returns(grpcCommandService)
    }

    @Test
    fun `success, event`() {
        // Given
        val lastRevision = 10
        val command = modelCommand(aggId = 1)
        val event = modelEvent(eventId = 5, aggId = 1, revision = 11)
        val remoteRequest = givenACommandIsSuccessful(command, lastRevision, event)
        val executor = createExecutor()

        // When
        val result = executor.execute(command, lastRevision)

        // Then
        assertThat(result).isEqualTo(CommandExecutor.ExecutionResult.Success(
                newEventMetadata = CommandExecutor.EventMetadata(event)
        ))
        verifySequence {
            grpcCommandService.withDeadline(grpcDeadline)
            grpcCommandService.postCommand(remoteRequest)
        }
    }

    @Test
    fun `success, no event`() {
        // Given
        val lastRevision = 10
        val command = modelCommand(aggId = 1)
        val remoteRequest = givenACommandIsSuccessful(command, lastRevision, null)
        val executor = createExecutor()

        // When
        val result = executor.execute(command, lastRevision)

        // Then
        assertThat(result).isEqualTo(CommandExecutor.ExecutionResult.Success(
                newEventMetadata = null
        ))
        verifySequence {
            grpcCommandService.withDeadline(grpcDeadline)
            grpcCommandService.postCommand(remoteRequest)
        }
    }

    @Test
    fun `success, but missing aggregate id`() {
        // Given
        val lastRevision = 10
        val command = modelCommand(aggId = 1)
        val event = modelEvent(eventId = 5, aggId = 1, revision = 11)
        val response = info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse
                .newBuilder()
                .setStatus(info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse.Status.SUCCESS)
                .setNewEventId(event.eventId)
                // missing aggregate id
                .setNewRevision(event.revision)
                .build()
        givenACommandResponse(command, lastRevision, response)
        val executor = createExecutor()

        // When
        val result = executor.execute(command, lastRevision)

        // Then
        assertThat(result).isEqualTo(CommandExecutor.ExecutionResult.Failure)
    }

    @Test
    fun `success, but missing revision`() {
        // Given
        val lastRevision = 10
        val command = modelCommand(aggId = 1)
        val event = modelEvent(eventId = 5, aggId = 1, revision = 11)
        val response = info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse
                .newBuilder()
                .setStatus(info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse.Status.SUCCESS)
                .setNewEventId(event.eventId)
                .setAggregateId(event.aggId)
                // missing revision
                .build()
        givenACommandResponse(command, lastRevision, response)
        val executor = createExecutor()

        // When
        val result = executor.execute(command, lastRevision)

        // Then
        assertThat(result).isEqualTo(CommandExecutor.ExecutionResult.Failure)
    }

    @Test
    fun `failure, remote`() {
        // Given
        val lastRevision = 10
        val command = modelCommand(aggId = 1)
        val remoteRequest = givenACommandFails(command, lastRevision)
        val executor = createExecutor()

        // When
        val result = executor.execute(command, lastRevision)

        // Then
        assertThat(result).isEqualTo(CommandExecutor.ExecutionResult.Failure)
        verifySequence {
            grpcCommandService.withDeadline(grpcDeadline)
            grpcCommandService.postCommand(remoteRequest)
        }
    }

    @Test
    fun `failure, local`() {
        // Given
        val lastRevision = 10
        val command = modelCommand(aggId = 1)
        every { grpcCommandMapper.toGrpcPostCommandRequest(any(), lastRevision) }.throws(IllegalArgumentException())
        val executor = createExecutor()

        // When
        val result = executor.execute(command, lastRevision)

        // Then
        assertThat(result).isEqualTo(CommandExecutor.ExecutionResult.Failure)
    }

    private fun createExecutor() =
            RemoteCommandExecutor(
                    grpcCommandMapper,
                    grpcCommandService,
                    grpcDeadline
            )

    private fun givenACommandFails(command: Command, lastRevision: Int): info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandRequest =
            givenACommandResponse(command, lastRevision, remoteError())

    private fun givenACommandIsSuccessful(command: Command, lastRevision: Int, event: Event?): info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandRequest =
            givenACommandResponse(command, lastRevision, remoteSuccess(event))

    private fun givenACommandResponse(command: Command, lastRevision: Int, response: info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse): info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandRequest {
        val remoteRequest: info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandRequest = mockk()
        every { grpcCommandMapper.toGrpcPostCommandRequest(command, lastRevision) }.returns(remoteRequest)
        every { grpcCommandService.postCommand(remoteRequest) }.returns(response)
        return remoteRequest
    }

    companion object {
        internal fun modelCommand(aggId: Int): Command =
                CreateNoteCommand("note-$aggId", path = Path("path-$aggId"), title = "Title $aggId", content = "Text $aggId")

        internal fun modelEvent(eventId: Int, aggId: Int, revision: Int): NoteCreatedEvent =
                NoteCreatedEvent(eventId = eventId, aggId = "note-$aggId", revision = revision, path = Path("path-$aggId"), title = "Title $aggId", content = "Text $aggId")

        internal fun remoteSuccess(event: Event?): info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse {
            return info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse
                    .newBuilder()
                    .setStatus(info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse.Status.SUCCESS)
                    .let {
                        if (event != null) {
                            it
                                    .setNewEventId(event.eventId)
                                    .setAggregateId(event.aggId)
                                    .setNewRevision(event.revision)

                        } else {
                            it
                        }
                    }
                    .build()
        }

        internal fun remoteError(): info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse {
            return info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse
                    .newBuilder()
                    .setStatus(info.maaskant.wmsnotes.server.command.grpc.Command.PostCommandResponse.Status.INTERNAL_ERROR).build()
        }
    }
}
