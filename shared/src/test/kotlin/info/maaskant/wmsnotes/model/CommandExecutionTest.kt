package info.maaskant.wmsnotes.model

import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal class CommandExecutionTest {
    private lateinit var commandRequests: PublishSubject<CommandRequest>
    private lateinit var commandResults: PublishSubject<CommandResult>
    private lateinit var commandBus: CommandBus

    @BeforeEach
    fun init() {
        commandBus = CommandBus()
    }

    @Test
    fun `executeBlocking, normal`() {
        // Given
        val request1 = TestCommandRequest(1)
        val request2 = TestCommandRequest(2)
        val result1 = CommandResult.Success(1)
        val result2 = CommandResult.Success(2)
        val commandRequestObserver = commandRequests.test()
        commandRequests
                .map {
                    when (it) {
                        request1 -> result1
                        request2 -> result2
                        else -> throw  IllegalArgumentException()
                    }
                }
                .subscribe(commandResults)

        // When
        val actualResult = CommandExecution.executeBlocking(commandBus, request2, 100, TimeUnit.MILLISECONDS)

        // Then
        assertThat(commandRequestObserver.values().toList()).isEqualTo(listOf(request2))
        assertThat(actualResult).isEqualTo(result2)
    }

    @Test
    fun `executeBlocking, timeout`() {
        // Given
        val request = TestCommandRequest(1)

        // When / then
        try {
            CommandExecution.executeBlocking(commandBus, request, 100, TimeUnit.MILLISECONDS)
        } catch (e: RuntimeException) {
            assertThat(e.cause is TimeoutException)
        }
    }

    private data class TestCommandRequest(override val requestId: Int) : CommandRequest
}