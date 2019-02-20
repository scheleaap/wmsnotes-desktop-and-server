package info.maaskant.wmsnotes.client.synchronization.commandexecutor

import info.maaskant.wmsnotes.model.*
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class LocalCommandExecutorTest {
    private val commandProcessor: CommandProcessor = mockk()

    @BeforeEach
    fun init() {
        clearMocks(
                commandProcessor
        )
    }

    @Test
    fun `success, event`() {
        // Given
        val command = modelCommand(noteId = 1, lastRevision = 10)
        val event = modelEvent(eventId = 5, noteId = 1, revision = 11)
        givenACommandProducesAnEvent(command, event)
        val executor = createExecutor()

        // When
        val result = executor.execute(command)

        // Then
        assertThat(result).isEqualTo(CommandExecutor.ExecutionResult.Success(
                newEventMetadata = CommandExecutor.EventMetadata(event)
        ))
        verifySequence {
            commandProcessor.blockingProcessCommand(command)
        }
    }

    @Test
    fun `success, no event`() {
        // Given
        val command = modelCommand(noteId = 1, lastRevision = 10)
        givenACommandProducesAnEvent(command, null)
        val executor = createExecutor()

        // When
        val result = executor.execute(command)

        // Then
        assertThat(result).isEqualTo(CommandExecutor.ExecutionResult.Success(
                newEventMetadata = null
        ))
        verifySequence {
            commandProcessor.blockingProcessCommand(command)
        }
    }

    @Test
    fun failure() {
        // Given
        val command = modelCommand(noteId = 1, lastRevision = 10)
        givenACommandFails(command)
        val executor = createExecutor()

        // When
        val result = executor.execute(command)

        // Then
        assertThat(result).isEqualTo(CommandExecutor.ExecutionResult.Failure)
        verifySequence {
            commandProcessor.blockingProcessCommand(command)
        }
    }

    private fun createExecutor() =
            LocalCommandExecutor(commandProcessor)

    private fun givenACommandProducesAnEvent(command: Command, event: Event?) {
        every { commandProcessor.blockingProcessCommand(command) }.returns(event)
    }

    private fun givenACommandFails(command: Command) {
        every { commandProcessor.blockingProcessCommand(command) }.throws(RuntimeException())
    }

    companion object {
        internal fun modelCommand(noteId: Int, lastRevision: Int? = null): Command {
            return if (lastRevision == null) {
                CreateNoteCommand("note-$noteId", "Title $noteId")
            } else {
                DeleteNoteCommand("note-$noteId", lastRevision)
            }
        }

        internal fun modelEvent(eventId: Int, noteId: Int, revision: Int): NoteCreatedEvent {
            return NoteCreatedEvent(eventId = eventId, noteId = "note-$noteId", revision = revision, title = "Title $noteId")
        }
    }
}
