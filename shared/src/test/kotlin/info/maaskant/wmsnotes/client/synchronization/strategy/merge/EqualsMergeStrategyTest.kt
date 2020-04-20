package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import assertk.assertThat
import assertk.assertions.isEqualTo
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.Solution
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.testutilities.Toastie
import io.mockk.mockk
import org.junit.jupiter.api.Test

@Suppress("UnnecessaryVariable")
internal class EqualsMergeStrategyTest {
    private val aggId = "n-10000000-0000-0000-0000-000000000000"

    @Test
    fun `exactly equal`() {
        // Given
        val localEvent1: Event = mockk()
        val localEvent2: Event = mockk()
        val localEvents = listOf(localEvent1, localEvent2)
        val remoteEvent1: Event = mockk()
        val remoteEvent2: Event = mockk()
        val remoteEvents = listOf(remoteEvent1, remoteEvent2)
        val baseAggregate: Toastie = Toastie(aggId = aggId, revision = 1, value = 1)
        val localAggregate: Toastie = Toastie(aggId = aggId, revision = 2, value = 2)
        val remoteAggregate = localAggregate
        val strategy = createInstance()

        // When
        val result = strategy.merge(localEvents, remoteEvents, baseAggregate, localAggregate, remoteAggregate)

        // Then
        assertThat(result).isEqualTo(Solution(
                newLocalEvents = emptyList(),
                newRemoteEvents = emptyList()
        ))
    }

    @Test
    fun `equal, but different revisions`() {
        // Given
        val localEvent1: Event = mockk()
        val localEvent2: Event = mockk()
        val localEvents = listOf(localEvent1, localEvent2)
        val remoteEvent1: Event = mockk()
        val remoteEvent2: Event = mockk()
        val remoteEvents = listOf(remoteEvent1, remoteEvent2)
        val baseNote = Toastie(aggId = aggId, revision = 1, value = 1)
        val localAggregate = Toastie(aggId = aggId, revision = 2, value = 2)
        val remoteAggregate = Toastie(aggId = aggId, revision = 3, value = 2)
        val strategy = createInstance()

        // When
        val result = strategy.merge(localEvents, remoteEvents, baseNote, localAggregate, remoteAggregate)

        // Then
        assertThat(result).isEqualTo(Solution(
                newLocalEvents = emptyList(),
                newRemoteEvents = emptyList()
        ))
    }

    @Test
    fun `not equal`() {
        // Given
        val localEvent1: Event = mockk()
        val localEvent2: Event = mockk()
        val localEvents = listOf(localEvent1, localEvent2)
        val remoteEvent1: Event = mockk()
        val remoteEvent2: Event = mockk()
        val remoteEvents = listOf(remoteEvent1, remoteEvent2)
        val baseAggregate = Toastie(aggId = aggId, revision = 1, value = 1)
        val localAggregate = Toastie(aggId = aggId, revision = 2, value = 2)
        val remoteNote = Toastie(aggId = aggId, revision = 2, value = 3)
        val strategy = createInstance()

        // When
        val result = strategy.merge(localEvents, remoteEvents, baseAggregate, localAggregate, remoteNote)

        // Then
        assertThat(result).isEqualTo(NoSolution)
    }

    private fun createInstance() =
            EqualsMergeStrategy<Toastie>()
}

