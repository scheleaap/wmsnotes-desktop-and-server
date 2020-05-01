package info.maaskant.wmsnotes.client.synchronization.strategy

import assertk.assertThat
import assertk.assertions.isEqualTo
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult.Solution
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.note.TitleChangedEvent
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class SkippingIdenticalDelegatingSynchronizationStrategyTest {
    private val delegate: SynchronizationStrategy = mockk()

    @BeforeEach
    fun init() {
        clearMocks(delegate)
        every { delegate.resolve(any(), any(), any()) }.returns(NoSolution)
    }

    @TestFactory
    fun `delegate everything`(): List<DynamicTest> {
        val items = listOf(
                Triple("1 local event, 0 remote events",
                        listOf(event(eventId = 1, revision = 1, title = 1)),
                        emptyList<Event>()
                ),
                Triple("0 local events, 1 remote event",
                        emptyList<Event>(),
                        listOf(event(eventId = 1, revision = 1, title = 1))
                ),
                Triple("1 local event, 1 remote event, different content",
                        listOf(event(eventId = 1, revision = 1, title = 1)),
                        listOf(event(eventId = 1, revision = 1, title = 2))
                )
        )
        return items.map { (name, localEvents, remoteEvents) ->
            DynamicTest.dynamicTest(name) {
                // Given
                val delegateSolution: ResolutionResult = givenDelegateCanResolve(localEvents, remoteEvents)
                val strategy = createInstance()

                // When
                val result = strategy.resolve(aggId = aggId, localEvents = localEvents, remoteEvents = remoteEvents)

                // Then
                assertThat(result).isEqualTo(delegateSolution)
            }
        }
    }

    @TestFactory
    fun `empty solution scenarios`(): List<DynamicTest> {
        val items = listOf(
                Triple("no events", emptyList<Event>(), emptyList<Event>()),
                Triple("1 local event, 1 remote event, same content, different eventId",
                        listOf(event(eventId = 1, revision = 1, title = 1)),
                        listOf(event(eventId = 11, revision = 1, title = 1))
                ),
                Triple("1 local event, 1 remote event, same content, different revision",
                        listOf(event(eventId = 1, revision = 1, title = 1)),
                        listOf(event(eventId = 1, revision = 11, title = 1))
                )
        )
        return items.map { (name, localEvents, remoteEvents) ->
            DynamicTest.dynamicTest(name) {
                // Given
                val strategy = createInstance()

                // When
                val result = strategy.resolve(aggId = aggId, localEvents = localEvents, remoteEvents = remoteEvents)

                // Then
                assertThat(result).isEqualTo(Solution(
                        compensatedLocalEvents = localEvents,
                        compensatedRemoteEvents = remoteEvents,
                        newLocalEvents = emptyList(),
                        newRemoteEvents = emptyList()
                ))
            }
        }
    }

    @Test
    fun `drop first events that are equal`() {
        // Given
        val localEvents = listOf(
                event(eventId = 1, revision = 1, title = 1),
                event(eventId = 2, revision = 2, title = 2),
                event(eventId = 3, revision = 3, title = 3)
        )
        val remoteEvents = listOf(
                event(eventId = 11, revision = 11, title = 1),
                event(eventId = 12, revision = 12, title = 2),
                event(eventId = 13, revision = 13, title = 4)
        )
        val delegateSolution: Solution = givenDelegateCanResolve(localEvents.drop(2), remoteEvents.drop(2))
        val strategy = createInstance()

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(Solution(
                compensatedLocalEvents = localEvents,
                compensatedRemoteEvents = remoteEvents,
                newLocalEvents = delegateSolution.newLocalEvents,
                newRemoteEvents = delegateSolution.newRemoteEvents
        ))
    }

    @Test
    fun `do not drop same events after the first different event`() {
        // Given
        val localEvents = listOf(
                event(eventId = 1, revision = 1, title = 1),
                event(eventId = 2, revision = 2, title = 2),
                event(eventId = 3, revision = 3, title = 4)
        )
        val remoteEvents = listOf(
                event(eventId = 11, revision = 11, title = 1),
                event(eventId = 12, revision = 12, title = 3),
                event(eventId = 13, revision = 13, title = 4)
        )
        val delegateSolution: Solution = givenDelegateCanResolve(localEvents.drop(1), remoteEvents.drop(1))
        val strategy = createInstance()

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(Solution(
                compensatedLocalEvents = localEvents,
                compensatedRemoteEvents = remoteEvents,
                newLocalEvents = delegateSolution.newLocalEvents,
                newRemoteEvents = delegateSolution.newRemoteEvents
        ))
    }

    @Test
    fun `more local than remote events`() {
        // Given
        val localEvents = listOf(
                event(eventId = 1, revision = 1, title = 1),
                event(eventId = 2, revision = 2, title = 2)
        )
        val remoteEvents = listOf(
                event(eventId = 11, revision = 11, title = 1),
                event(eventId = 12, revision = 12, title = 3),
                event(eventId = 13, revision = 13, title = 4)
        )
        val delegateSolution: Solution = givenDelegateCanResolve(localEvents.drop(1), remoteEvents.drop(1))
        val strategy = createInstance()

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(Solution(
                compensatedLocalEvents = localEvents,
                compensatedRemoteEvents = remoteEvents,
                newLocalEvents = delegateSolution.newLocalEvents,
                newRemoteEvents = delegateSolution.newRemoteEvents
        ))
    }

    @Test
    fun `more remote than local events`() {
        // Given
        val localEvents = listOf(
                event(eventId = 1, revision = 1, title = 1),
                event(eventId = 2, revision = 2, title = 2),
                event(eventId = 3, revision = 3, title = 4)
        )
        val remoteEvents = listOf(
                event(eventId = 11, revision = 11, title = 1),
                event(eventId = 12, revision = 12, title = 3)
        )
        val delegateSolution: Solution = givenDelegateCanResolve(localEvents.drop(1), remoteEvents.drop(1))
        val strategy = createInstance()

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(Solution(
                compensatedLocalEvents = localEvents,
                compensatedRemoteEvents = remoteEvents,
                newLocalEvents = delegateSolution.newLocalEvents,
                newRemoteEvents = delegateSolution.newRemoteEvents
        ))
    }


    private fun createInstance() = SkippingIdenticalDelegatingSynchronizationStrategy(delegate)

    private fun givenDelegateCanResolve(localEvents: List<Event>, remoteEvents: List<Event>): Solution {
        val solution = Solution(
                compensatedLocalEvents = localEvents,
                compensatedRemoteEvents = remoteEvents,
                newLocalEvents = listOf(mockk(), mockk()),
                newRemoteEvents = listOf(mockk(), mockk())
        )
        every { delegate.resolve(aggId = aggId, localEvents = localEvents, remoteEvents = remoteEvents) }.returns(solution)
        return solution
    }

    companion object {
        private const val aggId = "agg"

        internal fun event(eventId: Int, revision: Int, title: Int): TitleChangedEvent {
            return TitleChangedEvent(eventId = eventId, aggId = aggId, revision = revision, title = "Title $title")
        }
    }
}