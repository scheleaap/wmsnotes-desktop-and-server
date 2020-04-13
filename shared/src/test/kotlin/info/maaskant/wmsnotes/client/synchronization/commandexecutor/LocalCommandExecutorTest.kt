package info.maaskant.wmsnotes.client.synchronization.commandexecutor

import arrow.core.Either.Companion.left
import arrow.core.Either.Companion.right
import arrow.core.Option
import assertk.assertThat
import assertk.assertions.isEqualTo
import info.maaskant.wmsnotes.client.synchronization.CommandToCommandRequestMapper
import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.note.CreateNoteCommand
import info.maaskant.wmsnotes.model.note.NoteCommand
import info.maaskant.wmsnotes.model.note.NoteCommandRequest
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import info.maaskant.wmsnotes.testutilities.ExecutionResultAssertions.isFailure
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.observers.TestObserver
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

@Suppress("SameParameterValue")
internal class LocalCommandExecutorTest {
    private lateinit var commandBus: CommandBus
    private lateinit var requestsObserver: TestObserver<CommandRequest<Command>>
    private val commandToCommandRequestMapper: CommandToCommandRequestMapper = mockk()

    @BeforeEach
    fun init() {
        clearMocks(
                commandToCommandRequestMapper
        )
        commandBus = CommandBus()
        requestsObserver = commandBus.requests.test()
    }

    @Test
    fun `success, event`() {
        // Given
        val lastRevision = 10
        val command = modelCommand(aggId = 1)
        val request = givenACommandMapsToARequest(command, lastRevision)
        val event = modelEvent(eventId = 5, aggId = 1, revision = lastRevision + 1)
        givenARequestCanBeExecutedSuccessfully(request, event)
        val executor = createExecutor()

        // When
        val result = executor.execute(command, lastRevision = lastRevision)

        // Then
        assertThat(result).isEqualTo(CommandExecutor.ExecutionResult.Success(
                newEventMetadata = CommandExecutor.EventMetadata(event)
        ))
        assertThat(requestsObserver.values().toList()).isEqualTo(listOf(request))
    }

    @Test
    fun `success, no event`() {
        // Given
        val lastRevision = 10
        val command = modelCommand(aggId = 1)
        val request = givenACommandMapsToARequest(command, lastRevision)
        givenARequestCanBeExecutedSuccessfully(request, null)
        val executor = createExecutor()

        // When
        val result = executor.execute(command, lastRevision = 10)

        // Then
        assertThat(result).isEqualTo(CommandExecutor.ExecutionResult.Success(
                newEventMetadata = null
        ))
        assertThat(requestsObserver.values().toList()).isEqualTo(listOf(request))
    }

    @Test
    fun `failure, no result`() {
        // Given
        val lastRevision = 10
        val command = modelCommand(aggId = 1)
        val request = givenACommandMapsToARequest(command, lastRevision)
        val executor = createExecutor()

        // When
        val result = executor.execute(command, lastRevision = lastRevision)

        // Then
        assertThat(result).isFailure()
        assertThat(requestsObserver.values().toList()).isEqualTo(listOf(request))
    }

    @Test
    fun `failure, mapping failed`() {
        // Given
        val lastRevision = 10
        val command = modelCommand(aggId = 1)
        every { commandToCommandRequestMapper.map(any(), any(), any()) }.throws(IllegalArgumentException())
        val executor = createExecutor()

        // When
        val result = executor.execute(command, lastRevision = lastRevision)

        // Then
        assertThat(result).isFailure()
        assertThat(requestsObserver.values().toList()).isEqualTo(emptyList<CommandRequest<*>>())
    }

    @Test
    fun `failure, execution failed`() {
        // Given
        val lastRevision = 10
        val command = modelCommand(aggId = 1)
        val request = givenACommandMapsToARequest(command, lastRevision)
        givenARequestCannotBeExecutedSuccessfully(request)
        val executor = createExecutor()

        // When
        val result = executor.execute(command, lastRevision = lastRevision)

        // Then
        assertThat(result).isFailure()
        assertThat(requestsObserver.values().toList()).isEqualTo(listOf(request))
    }

    private fun createExecutor() =
            LocalCommandExecutor(commandToCommandRequestMapper, commandBus, timeout = CommandExecution.Duration(250, TimeUnit.MILLISECONDS))

    private fun givenACommandMapsToARequest(command: NoteCommand, lastRevision: Int): NoteCommandRequest {
        val request = NoteCommandRequest(
                aggId = command.aggId,
                commands = listOf(command),
                lastRevision = lastRevision,
                origin = CommandOrigin.REMOTE
        )
        every { commandToCommandRequestMapper.map(command, lastRevision, CommandOrigin.REMOTE) }.returns(request)
        return request
    }

    private fun givenARequestCanBeExecuted(request: NoteCommandRequest, success: Boolean, event: Event?) {
        val result = CommandResult(
                requestId = request.requestId,
                outcome = request.commands.map {
                    it to if (success) right(Option.fromNullable(event)) else left(CommandError.OtherError("test"))
                }.toImmutableList(),
                origin = CommandOrigin.REMOTE
        )
        commandBus.requests
                .map {
                    when (it) {
                        request -> result
                        else -> throw  IllegalArgumentException()
                    }
                }
                .subscribe(commandBus.results)
    }

    private fun givenARequestCanBeExecutedSuccessfully(request: NoteCommandRequest, event: Event?) =
            givenARequestCanBeExecuted(request = request, success = true, event = event)

    private fun givenARequestCannotBeExecutedSuccessfully(request: NoteCommandRequest) =
            givenARequestCanBeExecuted(request = request, success = false, event = null)

    companion object {
        internal fun modelCommand(aggId: Int): NoteCommand {
            return CreateNoteCommand("note-$aggId", path = Path("path-$aggId"), title = "Title $aggId", content = "Text $aggId")
        }

        internal fun modelEvent(eventId: Int, aggId: Int, revision: Int): NoteCreatedEvent {
            return NoteCreatedEvent(eventId = eventId, aggId = "note-$aggId", revision = revision, path = Path("path-$aggId"), title = "Title $aggId", content = "Text $aggId")
        }
    }
}
