package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import assertk.assertThat
import assertk.assertions.isEqualTo
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.Solution
import info.maaskant.wmsnotes.model.Aggregate
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal abstract class MergingSynchronizationStrategyTest<AggregateType : Aggregate<AggregateType>, EventType : Event> {
    private val aggregateRepository: AggregateRepository<AggregateType> = mockk()
    private val mergeStrategy: MergeStrategy<AggregateType> = mockk()

    @BeforeEach
    fun init() {
        clearMocks(
                aggregateRepository,
                mergeStrategy
        )
    }

    @Test
    fun solution() {
        // Given
        val baseRevision = 1
        val compensatedLocalEvent1: Event = createEvent(eventId = 1, aggId = 1, revision = baseRevision + 1)
        val compensatedLocalEvent2: Event = createEvent(eventId = 2, aggId = 1, revision = baseRevision + 2)
        val compensatedRemoteEvent1: Event = createEvent(eventId = 11, aggId = 1, revision = 11)
        val compensatedRemoteEvent2: Event = createEvent(eventId = 12, aggId = 1, revision = 12)
        val modifiedCompensatedRemoteEvent1: Event = createEvent(eventId = 11, aggId = 1, revision = baseRevision + 1)
        val modifiedCompensatedRemoteEvent2: Event = createEvent(eventId = 12, aggId = 1, revision = baseRevision + 2)
        val compensatedLocalEvents = listOf(compensatedLocalEvent1, compensatedLocalEvent2)
        val compensatedRemoteEvents = listOf(compensatedRemoteEvent1, compensatedRemoteEvent2)
        val modifiedCompensatedRemoteEvents = listOf(modifiedCompensatedRemoteEvent1, modifiedCompensatedRemoteEvent2)
        val newLocalEvent1: Event = createMockedEvent()
        val newLocalEvent2: Event = createMockedEvent()
        val newRemoteEvent1: Event = createMockedEvent()
        val newRemoteEvent2: Event = createMockedEvent()
        val aggId = compensatedLocalEvent1.aggId
        val baseAggregate: AggregateType = createMockedAggregate()
        val localAggregate: AggregateType = createMockedAggregate()
        val remoteAggregate: AggregateType = createMockedAggregate()
        every { aggregateRepository.get(aggId, baseRevision) }.returns(baseAggregate)
        givenAnEventApplicationResult(baseAggregate, compensatedLocalEvents, localAggregate)
        givenAnEventApplicationResult(baseAggregate, modifiedCompensatedRemoteEvents, remoteAggregate)
        every { mergeStrategy.merge(compensatedLocalEvents, compensatedRemoteEvents, baseAggregate, localAggregate, remoteAggregate) }.returns(Solution(
                newLocalEvents = listOf(newLocalEvent1, newLocalEvent2),
                newRemoteEvents = listOf(newRemoteEvent1, newRemoteEvent2)
        ))
        val strategy = createInstance(mergeStrategy, aggregateRepository)

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = compensatedLocalEvents, remoteEvents = compensatedRemoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.Solution(
                compensatedLocalEvents = compensatedLocalEvents,
                compensatedRemoteEvents = compensatedRemoteEvents,
                newLocalEvents = listOf(newLocalEvent1, newLocalEvent2),
                newRemoteEvents = listOf(newRemoteEvent1, newRemoteEvent2)
        ))
    }

    @Test
    fun `no solution`() {
        // Given
        val baseRevision = 1
        val compensatedLocalEvent1: Event = createEvent(eventId = 1, aggId = 1, revision = baseRevision + 1)
        val compensatedLocalEvent2: Event = createEvent(eventId = 2, aggId = 1, revision = baseRevision + 2)
        val compensatedRemoteEvent1: Event = createEvent(eventId = 11, aggId = 1, revision = 11)
        val compensatedRemoteEvent2: Event = createEvent(eventId = 12, aggId = 1, revision = 12)
        val modifiedCompensatedRemoteEvent1: Event = createEvent(eventId = 11, aggId = 1, revision = baseRevision + 1)
        val modifiedCompensatedRemoteEvent2: Event = createEvent(eventId = 12, aggId = 1, revision = baseRevision + 2)
        val compensatedLocalEvents = listOf(compensatedLocalEvent1, compensatedLocalEvent2)
        val compensatedRemoteEvents = listOf(compensatedRemoteEvent1, compensatedRemoteEvent2)
        val modifiedCompensatedRemoteEvents = listOf(modifiedCompensatedRemoteEvent1, modifiedCompensatedRemoteEvent2)
        val aggId = compensatedLocalEvent1.aggId
        val baseAggregate: AggregateType = createMockedAggregate()
        val localAggregate: AggregateType = createMockedAggregate()
        val remoteAggregate: AggregateType = createMockedAggregate()
        every { aggregateRepository.get(aggId, baseRevision) }.returns(baseAggregate)
        givenAnEventApplicationResult(baseAggregate, compensatedLocalEvents, localAggregate)
        givenAnEventApplicationResult(baseAggregate, modifiedCompensatedRemoteEvents, remoteAggregate)
        every { mergeStrategy.merge(compensatedLocalEvents, compensatedRemoteEvents, baseAggregate, localAggregate, remoteAggregate) }.returns(NoSolution)
        val strategy = createInstance(mergeStrategy, aggregateRepository)

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = compensatedLocalEvents, remoteEvents = compensatedRemoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.NoSolution)
    }

    @Test
    fun `no local events`() {
        // Given
        val baseRevision = 1
        val compensatedRemoteEvent1: Event = createEvent(eventId = 11, aggId = 1, revision = 11)
        val compensatedRemoteEvent2: Event = createEvent(eventId = 12, aggId = 1, revision = 12)
        val modifiedCompensatedRemoteEvent1: Event = createEvent(eventId = 11, aggId = 1, revision = baseRevision + 1)
        val modifiedCompensatedRemoteEvent2: Event = createEvent(eventId = 12, aggId = 1, revision = baseRevision + 2)
        val compensatedLocalEvents = emptyList<Event>()
        val compensatedRemoteEvents = listOf(compensatedRemoteEvent1, compensatedRemoteEvent2)
        val modifiedCompensatedRemoteEvents = listOf(modifiedCompensatedRemoteEvent1, modifiedCompensatedRemoteEvent2)
        val newLocalEvent1: Event = createMockedEvent()
        val newLocalEvent2: Event = createMockedEvent()
        val newRemoteEvent1: Event = createMockedEvent()
        val newRemoteEvent2: Event = createMockedEvent()
        val aggId = compensatedRemoteEvent1.aggId
        val baseAggregate: AggregateType = createMockedAggregate()
        val localAggregate: AggregateType = createMockedAggregate()
        val remoteAggregate: AggregateType = createMockedAggregate()
        givenAnEventApplicationResult(baseAggregate, modifiedCompensatedRemoteEvents, remoteAggregate)
        every { mergeStrategy.merge(compensatedLocalEvents, compensatedRemoteEvents, baseAggregate, localAggregate, remoteAggregate) }.returns(Solution(
                newLocalEvents = listOf(newLocalEvent1, newLocalEvent2),
                newRemoteEvents = listOf(newRemoteEvent1, newRemoteEvent2)
        ))
        val strategy = createInstance(mergeStrategy, aggregateRepository)

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = compensatedLocalEvents, remoteEvents = compensatedRemoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.NoSolution)
    }

    @Test
    fun `no remote events`() {
        // Given
        val baseRevision = 1
        val compensatedLocalEvent1: Event = createEvent(eventId = 1, aggId = 1, revision = baseRevision + 1)
        val compensatedLocalEvent2: Event = createEvent(eventId = 2, aggId = 1, revision = baseRevision + 2)
        val compensatedLocalEvents = listOf(compensatedLocalEvent1, compensatedLocalEvent2)
        val compensatedRemoteEvents = emptyList<Event>()
        val newLocalEvent1: Event = createMockedEvent()
        val newLocalEvent2: Event = createMockedEvent()
        val newRemoteEvent1: Event = createMockedEvent()
        val newRemoteEvent2: Event = createMockedEvent()
        val aggId = compensatedLocalEvent1.aggId
        val baseAggregate: AggregateType = createMockedAggregate()
        val localAggregate: AggregateType = createMockedAggregate()
        val remoteAggregate: AggregateType = createMockedAggregate()
        every { aggregateRepository.get(aggId, baseRevision) }.returns(baseAggregate)
        givenAnEventApplicationResult(baseAggregate, compensatedLocalEvents, localAggregate)
        every { mergeStrategy.merge(compensatedLocalEvents, compensatedRemoteEvents, baseAggregate, localAggregate, remoteAggregate) }.returns(Solution(
                newLocalEvents = listOf(newLocalEvent1, newLocalEvent2),
                newRemoteEvents = listOf(newRemoteEvent1, newRemoteEvent2)
        ))
        val strategy = createInstance(mergeStrategy, aggregateRepository)

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = compensatedLocalEvents, remoteEvents = compensatedRemoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.NoSolution)
    }

    @Test
    fun `another type of aggregate`() {
        // Given
        val baseRevision = 1
        val compensatedLocalEvent: Event = createOtherEvent(eventId = 1, aggId = 1, revision = baseRevision + 1)
        val compensatedRemoteEvent: Event = createOtherEvent(eventId = 11, aggId = 1, revision = 11)
        val compensatedLocalEvents = listOf(compensatedLocalEvent)
        val compensatedRemoteEvents = listOf(compensatedRemoteEvent)
        val aggId = compensatedLocalEvent.aggId
        val strategy = createInstance(mergeStrategy, aggregateRepository)

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = compensatedLocalEvents, remoteEvents = compensatedRemoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.NoSolution)
    }

    @Test
    fun `local event with revision 1`() {
        // This situation should not normally occur, because it would mean there is a conflict involving a local
        // aggregate that has just been created. Perhaps it could occur in case the client crashes during a previous
        // synchronization, though.

        // Given
        val baseRevision = 0 // 0 instead of 1!
        val compensatedLocalEvent1: Event = createEvent(eventId = 1, aggId = 1, revision = baseRevision + 1)
        val compensatedRemoteEvent1: Event = createEvent(eventId = 11, aggId = 1, revision = 11)
        val modifiedCompensatedRemoteEvent1: Event = createEvent(eventId = 11, aggId = 1, revision = baseRevision + 1)
        val compensatedLocalEvents = listOf(compensatedLocalEvent1)
        val compensatedRemoteEvents = listOf(compensatedRemoteEvent1)
        val modifiedCompensatedRemoteEvents = listOf(modifiedCompensatedRemoteEvent1)
        val newLocalEvent1: Event = createMockedEvent()
        val newRemoteEvent1: Event = createMockedEvent()
        val aggId = compensatedLocalEvent1.aggId
        val baseAggregate: AggregateType = createMockedAggregate()
        val localAggregate: AggregateType = createMockedAggregate()
        val remoteAggregate: AggregateType = createMockedAggregate()
        every { aggregateRepository.get(aggId, baseRevision) }.returns(baseAggregate)
        givenAnEventApplicationResult(baseAggregate, compensatedLocalEvents, localAggregate)
        givenAnEventApplicationResult(baseAggregate, modifiedCompensatedRemoteEvents, remoteAggregate)
        every { mergeStrategy.merge(compensatedLocalEvents, compensatedRemoteEvents, baseAggregate, localAggregate, remoteAggregate) }.returns(Solution(
                newLocalEvents = listOf(newLocalEvent1),
                newRemoteEvents = listOf(newRemoteEvent1)
        ))
        val strategy = createInstance(mergeStrategy, aggregateRepository)

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = compensatedLocalEvents, remoteEvents = compensatedRemoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.Solution(
                compensatedLocalEvents = listOf(compensatedLocalEvent1),
                compensatedRemoteEvents = listOf(compensatedRemoteEvent1),
                newLocalEvents = listOf(newLocalEvent1),
                newRemoteEvents = listOf(newRemoteEvent1)
        ))
    }

    private fun givenAnEventApplicationResult(
            baseAggregate: AggregateType,
            events: List<Event>,
            resultAggregate: AggregateType) {
        val last = events.last()
        val firsts = events - last
        var baseAggregateTemp = baseAggregate
        for (first in firsts) {
            val resultAggregateTemp: AggregateType = createMockedAggregate()
            every { baseAggregateTemp.apply(first) }.returns(Pair(resultAggregateTemp, first))
            baseAggregateTemp = resultAggregateTemp
        }
        every { baseAggregateTemp.apply(last) }.returns(Pair(resultAggregate, last))
    }

    abstract fun createInstance(mergeStrategy: MergeStrategy<AggregateType>, aggregateRepository: AggregateRepository<AggregateType>): MergingSynchronizationStrategy<AggregateType>
    abstract fun createEvent(eventId: Int, aggId: Int, revision: Int): EventType
    abstract fun createOtherEvent(eventId: Int, aggId: Int, revision: Int): Event
    abstract fun createMockedAggregate(): AggregateType
    abstract fun createMockedEvent(): EventType
}