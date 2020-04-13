package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.ManualMergeStrategy.ConflictResolutionChoice
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.ManualMergeStrategy.ConflictData
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.Solution
import info.maaskant.wmsnotes.model.note.Note
import io.mockk.*
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.DifferenceCompensator.CompensatingEvents
import info.maaskant.wmsnotes.model.Event
import org.junit.jupiter.api.Disabled

@Disabled("Tests written while traveling, code to be implemented next")
internal class ManualMergeStrategyTest {
    private val aggId = "note"

    private val differenceAnalyzer: DifferenceAnalyzer = mockk()
    private val differenceCompensator: DifferenceCompensator = mockk()

    @BeforeEach
    fun init() {
        clearMocks(
                differenceAnalyzer,
                differenceCompensator
        )
    }

    @Test
    fun `new merge request`() {
        // Given
        val localEvents: List<Event> = mockk()
        val remoteEvents: List<Event> = mockk()
        val baseNote: Note = createExistingNote(aggId)
        val localNote: Note = createExistingNote(aggId)
        val remoteNote: Note = createExistingNote(aggId)
        val strategy = createStrategy()
        val conflictObserver = strategy.getConflictedAggregateIds().test()

        // When
        val mergeResult = strategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote)
        val conflictData = strategy.getConflictData(aggId)

        // Then
        assertThat(mergeResult).isEqualTo(NoSolution)
        assertThat(conflictObserver.values().toList()).isEqualTo(listOf(emptySet(), setOf(aggId)))
        assertThat(conflictData).isEqualTo(ConflictData(
                baseNote = baseNote,
                localNote = localNote,
                remoteNote = remoteNote
        ))
    }

    @Test
    fun `resolve, local`() {
        // Given
        val localEvent1: Event = mockk()
        val localEvent2: Event = mockk()
        val localEvents = listOf(localEvent1, localEvent2)
        val remoteEvent1: Event = mockk()
        val remoteEvent2: Event = mockk()
        val remoteEvents = listOf(remoteEvent1, remoteEvent2)
        val baseNote: Note = createExistingNote(aggId)
        val localNote: Note = createExistingNote(aggId)
        val remoteNote: Note = createExistingNote(aggId)
        val compensatingEvent1: Event = mockk()
        val compensatingEvent2: Event = mockk()

        givenCompensatingEvents(aggId, givenDifferences(localNote, remoteNote), DifferenceCompensator.Target.LEFT, CompensatingEvents(
                leftEvents = emptyList(),
                rightEvents = listOf(compensatingEvent1, compensatingEvent1)
        ))
        val strategy = createStrategy()
        strategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote)
        val conflictObserver = strategy.getConflictedAggregateIds().test()

        // When
        strategy.resolve(aggId, ConflictResolutionChoice.LOCAL)
        val mergeResult = strategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote)

        // Then
        assertThat(mergeResult).isEqualTo(Solution(
                newLocalEvents = localEvents,
                newRemoteEvents = remoteEvents + listOf(compensatingEvent1, compensatingEvent2)
        ))
        assertThat(conflictObserver.values().toList()).isEqualTo(listOf(setOf(aggId), emptySet()))

        // When
        val mergeResult2 = strategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote)

        // Then
        assertThat(mergeResult2).isEqualTo(NoSolution)
    }

    @Test
    fun `resolve, right`() {
        // Given
        val localEvent1: Event = mockk()
        val localEvent2: Event = mockk()
        val localEvents = listOf(localEvent1, localEvent2)
        val remoteEvent1: Event = mockk()
        val remoteEvent2: Event = mockk()
        val remoteEvents = listOf(remoteEvent1, remoteEvent2)
        val baseNote: Note = createExistingNote(aggId)
        val localNote: Note = createExistingNote(aggId)
        val remoteNote: Note = createExistingNote(aggId)
        val compensatingEvent1: Event = mockk()
        val compensatingEvent2: Event = mockk()
        givenCompensatingEvents(aggId, givenDifferences(localNote, remoteNote), DifferenceCompensator.Target.RIGHT, CompensatingEvents(
                leftEvents = listOf(compensatingEvent1, compensatingEvent2),
                rightEvents = emptyList()
        ))
        val strategy = createStrategy()
        strategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote)
        val conflictObserver = strategy.getConflictedAggregateIds().test()

        // When
        strategy.resolve(aggId, ConflictResolutionChoice.REMOTE)
        val mergeResult = strategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote)

        // Then
        assertThat(mergeResult).isEqualTo(Solution(
                newLocalEvents = localEvents + listOf(compensatingEvent1, compensatingEvent2),
                newRemoteEvents = remoteEvents
        ))
        assertThat(conflictObserver.values().toList()).isEqualTo(listOf(setOf(aggId), emptySet()))

        // When
        val mergeResult2 = strategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote)

        // Then
        assertThat(mergeResult2).isEqualTo(NoSolution)
    }

    @Test
    fun `resolve, both`() {
        // Given
        val localEvent1: Event = mockk()
        val localEvent2: Event = mockk()
        val localEvents = listOf(localEvent1, localEvent2)
        val remoteEvent1: Event = mockk()
        val remoteEvent2: Event = mockk()
        val remoteEvents = listOf(remoteEvent1, remoteEvent2)
        val baseNote: Note = createExistingNote(aggId)
        val localNote: Note = createExistingNote(aggId)
        val remoteNote: Note = createExistingNote(aggId)
        val compensatingEvent1: Event = mockk()
        val compensatingEvent2: Event = mockk()
        val compensatingEvent3: Event = mockk()
        val compensatingEvent4: Event = mockk()
        givenCompensatingEvents(aggId, givenDifferences(localNote, remoteNote), DifferenceCompensator.Target.RIGHT, CompensatingEvents(
                leftEvents = listOf(compensatingEvent1, compensatingEvent2),
                rightEvents = emptyList()
        ))
        givenCompensatingEvents(aggId, givenDifferences(Note(), localNote), DifferenceCompensator.Target.RIGHT, CompensatingEvents(
                leftEvents = listOf(compensatingEvent3, compensatingEvent4),
                rightEvents = emptyList()
        ))
        val strategy = createStrategy()
        strategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote)
        val conflictObserver = strategy.getConflictedAggregateIds().test()

        // When
        strategy.resolve(aggId, ConflictResolutionChoice.BOTH)
        val mergeResult = strategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote)

        // Then
        assertThat(mergeResult).isEqualTo(Solution(
                newLocalEvents = localEvents + listOf(compensatingEvent1, compensatingEvent2, compensatingEvent3, compensatingEvent4),
                newRemoteEvents = remoteEvents + listOf(compensatingEvent3, compensatingEvent4)
        ))
        assertThat(conflictObserver.values().toList()).isEqualTo(listOf(setOf(aggId), emptySet()))

        // When
        val mergeResult2 = strategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote)

        // Then
        assertThat(mergeResult2).isEqualTo(NoSolution)
    }

    @Test
    fun `reset solution if events change`() {
        // Given
        val localEvents1: List<Event> = mockk()
        val localEvents2: List<Event> = mockk()
        val remoteEvents1: List<Event> = mockk()
        val remoteEvents2: List<Event> = mockk()
        val baseNote: Note = createExistingNote(aggId)
        val localNote1: Note = createExistingNote(aggId)
        val localNote2: Note = createExistingNote(aggId)
        val remoteNote1: Note = createExistingNote(aggId)
        val remoteNote2: Note = createExistingNote(aggId)
        val differences1: Set<Difference> = givenDifferences(localNote1, remoteNote1)
        val compensatingEventsForRemote1: List<Event> = mockk()
        givenCompensatingEvents(aggId, differences1, DifferenceCompensator.Target.LEFT, CompensatingEvents(
                leftEvents = localEvents1,
                rightEvents = compensatingEventsForRemote1
        ))
        val strategy = createStrategy()
        strategy.merge(localEvents1, remoteEvents1, baseNote, localNote1, remoteNote1)
        val conflictObserver = strategy.getConflictedAggregateIds().test()
        strategy.resolve(aggId, ConflictResolutionChoice.LOCAL)

        // When
        val mergeResult = strategy.merge(localEvents2, remoteEvents2, baseNote, localNote2, remoteNote2)

        // Then
        assertThat(mergeResult).isEqualTo(NoSolution)
        assertThat(conflictObserver.values().toList()).isEqualTo(listOf(setOf(aggId), emptySet(), setOf(aggId)))
    }

    private fun createStrategy() = ManualMergeStrategy(differenceAnalyzer, differenceCompensator)

    private fun givenCompensatingEvents(aggId: String, differences: Set<Difference>, target: DifferenceCompensator.Target, compensatingEvents: CompensatingEvents): CompensatingEvents {
        every { differenceCompensator.compensate(aggId, differences, target = target) }.returns(compensatingEvents)
        return compensatingEvents
    }

    private fun givenDifferences(localNote: Note, remoteNote: Note): Set<Difference> {
        val differences: Set<Difference> = mockk()
        every { differenceAnalyzer.compare(localNote, remoteNote) }.returns(differences)
        return differences
    }

    companion object {
        internal fun createExistingNote(aggId: String): Note {
            val note: Note = mockk()
            every { note.aggId }.returns(aggId)
            every { note.revision }.returns(5)
            every { note.exists }.returns(true)
            every { note.title }.returns("projected title")
            return note
        }
    }
}