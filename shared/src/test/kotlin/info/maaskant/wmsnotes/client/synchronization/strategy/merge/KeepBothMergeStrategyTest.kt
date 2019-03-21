package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.DifferenceCompensator.CompensatingEvents
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.Solution
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.note.Note
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KeepBothMergeStrategyTest {
    private val aggId = "note"
    private val newAggId = "new-note"

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
    fun resolve() {
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
        val differences1: Set<Difference> = givenDifferences(Note(), localNote)
        every {
            differenceCompensator.compensate(
                    aggId = newAggId,
                    differences = differences1,
                    target = DifferenceCompensator.Target.RIGHT
            )
        }.returns(CompensatingEvents(
                leftEvents = listOf(compensatingEvent3, compensatingEvent4),
                rightEvents = emptyList()
        ))
        val strategy = createStrategy()

        // When
        val mergeResult = strategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote)

        // Then
        assertThat(mergeResult).isEqualTo(Solution(
                newLocalEvents = listOf(compensatingEvent1, compensatingEvent2, compensatingEvent3, compensatingEvent4),
                newRemoteEvents = listOf(compensatingEvent3, compensatingEvent4)
        ))
    }

    private fun createStrategy() = KeepBothMergeStrategy(differenceAnalyzer, differenceCompensator) { newAggId }

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