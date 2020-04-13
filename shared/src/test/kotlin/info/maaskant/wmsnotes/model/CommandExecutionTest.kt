package info.maaskant.wmsnotes.model

import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.random.Random

internal class CommandExecutionTest {
    private val timeout = CommandExecution.Duration(100, TimeUnit.MILLISECONDS)

    private lateinit var commandBus: CommandBus

    @BeforeEach
    fun init() {
        commandBus = CommandBus()
    }

    @Test
    fun `executeBlocking, normal`() {
        // Given
        val request1 = commandRequest(1)
        val request2 = commandRequest(2)
        val result1 = commandResult(1)
        val result2 = commandResult(2)
        val commandRequestObserver = commandBus.requests.test()
        commandBus.requests
                .map {
                    when (it) {
                        request1 -> result1
                        request2 -> result2
                        else -> throw  IllegalArgumentException()
                    }
                }
                .subscribe(commandBus.results)

        // When
        val actualResult = CommandExecution.executeBlocking(commandBus, request2, timeout)

        // Then
        assertThat(commandRequestObserver.values().toList()).isEqualTo(listOf(request2))
        assertThat(actualResult).isEqualTo(result2)
    }

    @Test
    fun `executeBlocking, timeout`() {
        // Given
        val request = commandRequest(1)

        // When / then
        try {
            CommandExecution.executeBlocking(commandBus, request, timeout)
        } catch (e: RuntimeException) {
            assertThat(e.cause is TimeoutException)
        }
    }

    private fun commandResult(requestId: Int) =
            CommandResult(requestId = requestId, outcome = persistentListOf(), origin = randomOrigin())

    private fun commandRequest(requestId: Int): CommandRequest<Command> {
        val request = mockk<CommandRequest<Command>>()
        every { request.requestId }.returns(requestId)
        return request
    }

    private fun randomOrigin(): CommandOrigin {
        val values = CommandOrigin.values()
        return values[Random.nextInt(values.size)]
    }
}