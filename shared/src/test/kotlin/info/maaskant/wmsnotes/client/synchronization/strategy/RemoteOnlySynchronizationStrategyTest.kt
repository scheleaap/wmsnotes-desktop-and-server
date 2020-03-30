package info.maaskant.wmsnotes.client.synchronization.strategy

import info.maaskant.wmsnotes.client.synchronization.CompensatingAction
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult.Solution
import info.maaskant.wmsnotes.model.Event
import io.mockk.mockk
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.Test

internal class RemoteOnlySynchronizationStrategyTest {
    private val aggId = "note"

    @Test
    fun `only remote events`() {
        // Given
        val event1: Event = mockk()
        val event2: Event = mockk()
        val localEvents = emptyList<Event>()
        val remoteEvents = listOf(event1, event2)
        val strategy = RemoteOnlySynchronizationStrategy()

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(Solution(listOf(
                CompensatingAction(
                        compensatedLocalEvents = emptyList(),
                        compensatedRemoteEvents = listOf(event1),
                        newLocalEvents = listOf(event1),
                        newRemoteEvents = emptyList()
                ),
                CompensatingAction(
                        compensatedLocalEvents = emptyList(),
                        compensatedRemoteEvents = listOf(event2),
                        newLocalEvents = listOf(event2),
                        newRemoteEvents = emptyList()
                )
        )))
    }

    @Test
    fun `local and remote events`() {
        // Given
        val event1: Event = mockk()
        val event2: Event = mockk()
        val event3: Event = mockk()
        val localEvents = listOf(event1)
        val remoteEvents = listOf(event2, event3)
        val strategy = RemoteOnlySynchronizationStrategy()

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(NoSolution)
    }

    @Test
    fun `no events`() {
        // Given
        val localEvents = emptyList<Event>()
        val remoteEvents = emptyList<Event>()
        val strategy = RemoteOnlySynchronizationStrategy()

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(Solution(emptyList()))
    }
}