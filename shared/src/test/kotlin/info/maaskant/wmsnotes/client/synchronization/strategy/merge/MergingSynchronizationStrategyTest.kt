package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.CompensatingAction
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.Solution
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.NoteProjector
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MergingSynchronizationStrategyTest {
    private val noteProjector: NoteProjector = mockk()
    private val mergeStrategy: MergeStrategy = mockk()

    @BeforeEach
    fun init() {
        clearMocks(
                noteProjector,
                mergeStrategy
        )
    }

    @Test
    fun solution() {
        // Given
        val oldLocalEvent1: Event = modelEvent(eventId = 1, noteId = 1, revision = 1)
        val oldLocalEvent2: Event = modelEvent(eventId = 2, noteId = 1, revision = 2)
        val oldRemoteEvent1: Event = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val oldRemoteEvent2: Event = modelEvent(eventId = 12, noteId = 1, revision = 2)
        val newLocalEvent1: Event = mockk()
        val newLocalEvent2: Event = mockk()
        val newRemoteEvent1: Event = mockk()
        val newRemoteEvent2: Event = mockk()
        val noteId = oldLocalEvent1.noteId
        val localEvents = listOf(oldLocalEvent1, oldLocalEvent2)
        val remoteEvents = listOf(oldRemoteEvent1, oldRemoteEvent2)
        val baseNote: Note = mockk()
        val localNote: Note = mockk()
        val remoteNote: Note = mockk()
        every { noteProjector.project(noteId, oldLocalEvent1.revision - 1) }.returns(baseNote)
        givenAnEventApplicationResult(baseNote, localNote, listOf(oldLocalEvent1, oldLocalEvent2))
        givenAnEventApplicationResult(baseNote, remoteNote, listOf(oldRemoteEvent1, oldRemoteEvent2))
        every { mergeStrategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote) }.returns(Solution(
                newLocalEvents = listOf(newLocalEvent1, newLocalEvent2),
                newRemoteEvents = listOf(newRemoteEvent1, newRemoteEvent2)
        ))
        val strategy = MergingSynchronizationStrategy(mergeStrategy, noteProjector)

        // When
        val result = strategy.resolve(noteId = noteId, localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.Solution(listOf(CompensatingAction(
                compensatedLocalEvents = listOf(oldLocalEvent1, oldLocalEvent2),
                compensatedRemoteEvents = listOf(oldRemoteEvent1, oldRemoteEvent2),
                newLocalEvents = listOf(newLocalEvent1, newLocalEvent2),
                newRemoteEvents = listOf(newRemoteEvent1, newRemoteEvent2)
        ))))
    }

    @Test
    fun `no solution`() {
        // Given
        val oldLocalEvent1: Event = modelEvent(eventId = 1, noteId = 1, revision = 1)
        val oldLocalEvent2: Event = modelEvent(eventId = 2, noteId = 1, revision = 1)
        val oldRemoteEvent1: Event = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val oldRemoteEvent2: Event = modelEvent(eventId = 12, noteId = 1, revision = 1)
        val noteId = oldLocalEvent1.noteId
        val localEvents = listOf(oldLocalEvent1, oldLocalEvent2)
        val remoteEvents = listOf(oldRemoteEvent1, oldRemoteEvent2)
        val baseNote: Note = mockk()
        val localNote: Note = mockk()
        val remoteNote: Note = mockk()
        every { noteProjector.project(noteId, oldLocalEvent1.revision - 1) }.returns(baseNote)
        givenAnEventApplicationResult(baseNote, localNote, listOf(oldLocalEvent1, oldLocalEvent2))
        givenAnEventApplicationResult(baseNote, remoteNote, listOf(oldRemoteEvent1, oldRemoteEvent2))
        every { mergeStrategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote) }.returns(NoSolution)
        val strategy = MergingSynchronizationStrategy(mergeStrategy, noteProjector)

        // When
        val result = strategy.resolve(noteId = noteId, localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.NoSolution)
    }

    @Test
    fun `no local events`() {
        // Given
        val oldRemoteEvent1: Event = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val oldRemoteEvent2: Event = modelEvent(eventId = 12, noteId = 1, revision = 1)
        val newLocalEvent1: Event = mockk()
        val newLocalEvent2: Event = mockk()
        val newRemoteEvent1: Event = mockk()
        val newRemoteEvent2: Event = mockk()
        val noteId = oldRemoteEvent1.noteId
        val localEvents = emptyList<Event>()
        val remoteEvents = listOf(oldRemoteEvent1, oldRemoteEvent2)
        val baseNote: Note = mockk()
        val localNote: Note = mockk()
        val remoteNote: Note = mockk()
        givenAnEventApplicationResult(baseNote, remoteNote, listOf(oldRemoteEvent1, oldRemoteEvent2))
        every { mergeStrategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote) }.returns(Solution(
                newLocalEvents = listOf(newLocalEvent1, newLocalEvent2),
                newRemoteEvents = listOf(newRemoteEvent1, newRemoteEvent2)
        ))
        val strategy = MergingSynchronizationStrategy(mergeStrategy, noteProjector)

        // When
        val result = strategy.resolve(noteId = noteId, localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.NoSolution)
    }

    @Test
    fun `no remote events`() {
        // Given
        val oldLocalEvent1: Event = modelEvent(eventId = 1, noteId = 1, revision = 1)
        val oldLocalEvent2: Event = modelEvent(eventId = 2, noteId = 1, revision = 1)
        val newLocalEvent1: Event = mockk()
        val newLocalEvent2: Event = mockk()
        val newRemoteEvent1: Event = mockk()
        val newRemoteEvent2: Event = mockk()
        val noteId = oldLocalEvent1.noteId
        val localEvents = listOf(oldLocalEvent1, oldLocalEvent2)
        val remoteEvents = emptyList<Event>()
        val baseNote: Note = mockk()
        val localNote: Note = mockk()
        val remoteNote: Note = mockk()
        every { noteProjector.project(noteId, oldLocalEvent1.revision - 1) }.returns(baseNote)
        givenAnEventApplicationResult(baseNote, localNote, listOf(oldLocalEvent1, oldLocalEvent2))
        every { mergeStrategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote) }.returns(Solution(
                newLocalEvents = listOf(newLocalEvent1, newLocalEvent2),
                newRemoteEvents = listOf(newRemoteEvent1, newRemoteEvent2)
        ))
        val strategy = MergingSynchronizationStrategy(mergeStrategy, noteProjector)

        // When
        val result = strategy.resolve(noteId = noteId, localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.NoSolution)
    }

    @Test
    fun `local event with revision 1`() {
        // This situation should not normally occur, because it would mean there is a conflict involving a local note
        // that has just been created. Perhaps it could occur in case the client crashes during a previous
        // synchronization, though.

        // Given
        val oldLocalEvent1: Event = modelEvent(eventId = 1, noteId = 1, revision = 1)
        val oldRemoteEvent1: Event = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val newLocalEvent1: Event = mockk()
        val newRemoteEvent1: Event = mockk()
        val noteId = oldLocalEvent1.noteId
        val localEvents = listOf(oldLocalEvent1)
        val remoteEvents = listOf(oldRemoteEvent1)
        val baseNote: Note = mockk()
        val localNote: Note = mockk()
        val remoteNote: Note = mockk()
        every { noteProjector.project(noteId, 0) }.returns(baseNote)
        every { baseNote.apply(oldLocalEvent1) }.returns(Pair(localNote, oldLocalEvent1))
        every { baseNote.apply(oldRemoteEvent1) }.returns(Pair(remoteNote, oldRemoteEvent1))
        every { mergeStrategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote) }.returns(Solution(
                newLocalEvents = listOf(newLocalEvent1),
                newRemoteEvents = listOf(newRemoteEvent1)
        ))
        val strategy = MergingSynchronizationStrategy(mergeStrategy, noteProjector)

        // When
        val result = strategy.resolve(noteId = noteId, localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.Solution(listOf(CompensatingAction(
                compensatedLocalEvents = listOf(oldLocalEvent1),
                compensatedRemoteEvents = listOf(oldRemoteEvent1),
                newLocalEvents = listOf(newLocalEvent1),
                newRemoteEvents = listOf(newRemoteEvent1)
        ))))
    }

    private fun givenAnEventApplicationResult(
            baseNote: Note,
            resultNote: Note,
            events: List<Event>) {
        val last = events.last()
        val firsts = events - last
        var baseNoteTemp = baseNote
        for (first in firsts) {
            val resultNoteTemp: Note = mockk()
            every { baseNoteTemp.apply(first) }.returns(Pair(resultNoteTemp, first))
            baseNoteTemp = resultNoteTemp
        }
        every { baseNoteTemp.apply(last) }.returns(Pair(resultNote, last))
    }

    companion object {
        internal fun modelEvent(eventId: Int, noteId: Int, revision: Int): NoteCreatedEvent {
            return NoteCreatedEvent(eventId = eventId, noteId = "note-$noteId", revision = revision, title = "Title $noteId")
        }
    }
}