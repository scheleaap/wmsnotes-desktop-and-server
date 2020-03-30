package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.Solution
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import info.maaskant.wmsnotes.model.note.TitleChangedEvent
import io.mockk.mockk
import assertk.assertThat
import assertk.assertions.*
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
        val baseNote: Note = Note()
                .apply(NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = Path("path"), title = "Title", content = "Content")).first
        val localNote: Note = baseNote
                .apply(TitleChangedEvent(eventId = 0, aggId = aggId, revision = 2, title = "v1")).first
        val remoteNote = localNote
        val strategy = createStrategy()

        // When
        val result = strategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote)

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
        val baseNote: Note = Note()
                .apply(NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = Path("path"), title = "Title", content = "Content")).first
        val localNote: Note = baseNote
                .apply(TitleChangedEvent(eventId = 0, aggId = aggId, revision = 2, title = "v1")).first
        val remoteNote = localNote
                .apply(TitleChangedEvent(eventId = 0, aggId = aggId, revision = 3, title = "v2")).first
                .apply(TitleChangedEvent(eventId = 0, aggId = aggId, revision = 4, title = "v1")).first
        val strategy = createStrategy()

        // When
        val result = strategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote)

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
        val baseNote: Note = Note()
                .apply(NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = Path("path"), title = "Title", content = "Content")).first
        val localNote: Note = baseNote
                .apply(TitleChangedEvent(eventId = 0, aggId = aggId, revision = 2, title = "v1")).first
        val remoteNote = baseNote
                .apply(TitleChangedEvent(eventId = 0, aggId = aggId, revision = 2, title = "v2")).first
        val strategy = createStrategy()

        // When
        val result = strategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote)

        // Then
        assertThat(result).isEqualTo(NoSolution)
    }

    private fun createStrategy() =
            EqualsMergeStrategy()
}