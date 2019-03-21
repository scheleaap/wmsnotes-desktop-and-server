package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.Solution
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.note.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.*

@Disabled("Tests written while traveling, code to be implemented next")
internal class SimpleMergeStrategyTest {
    private val aggId = "note"
    private val attachmentContent1 = "data 1".toByteArray()
    private val attachmentContent2 = "data 2".toByteArray()

    private val differenceAnalyzer: DifferenceAnalyzer = DifferenceAnalyzer()
    private val differenceCompensator: DifferenceCompensator = DifferenceCompensator()

    @BeforeEach
    fun init() {
        clearMocks(
                differenceAnalyzer,
                differenceCompensator
        )
    }

    @TestFactory
    fun test(): List<DynamicTest> {
        val items = listOf(
                Triple(
                        TitleChangedEvent(eventId = 0, aggId = aggId, revision = 2, title = "v1"),
                        listOf(TitleChangedEvent(eventId = 0, aggId = aggId, revision = 2, title = "v2")),
                        emptyList<Event>()
                ),
                Triple(
                        ContentChangedEvent(eventId = 0, aggId = aggId, revision = 2, content = "v1"),
                        listOf(ContentChangedEvent(eventId = 0, aggId = aggId, revision = 2, content = "v2")),
                        emptyList<Event>()
                ),
                Triple(
                        AttachmentAddedEvent(eventId = 0, aggId = aggId, revision = 2, name = "att-1", content = attachmentContent1),
                        listOf(
                                AttachmentAddedEvent(eventId = 0, aggId = aggId, revision = 2, name = "att-1", content = attachmentContent2),
                                AttachmentDeletedEvent(eventId = 0, aggId = aggId, revision = 2, name = "att-1")
                        ),
                        listOf(
                                AttachmentAddedEvent(eventId = 0, aggId = aggId, revision = 2, name = "att-2", content = attachmentContent1)
                        )
                )
                // Add more classes here
        )
        return items.map { (event, conflictingEvents, nonConflictingEvents) ->
            DynamicTest.dynamicTest(event::class.simpleName) {
                // Given
                val localEvent1: Event = mockk()
                val localEvent2: Event = mockk()
                val localEvents = listOf(localEvent1, localEvent2)
                val remoteEvent1: Event = mockk()
                val remoteEvent2: Event = mockk()
                val remoteEvents = listOf(remoteEvent1, remoteEvent2)
                val baseNote: Note = Note()
                        .apply(NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = Path("path"), title = "", content = "")).first
                val strategy = createStrategy()

                // When / then
                assertThat(event.revision).isEqualTo(2)
                assertThat(strategy.merge(localEvents, remoteEvents, baseNote, baseNote, baseNote)).isEqualTo(Solution(
                        newLocalEvents = localEvents,
                        newRemoteEvents = remoteEvents
                ))
                assertThat(strategy.merge(localEvents, remoteEvents, baseNote, baseNote.apply(event).first, baseNote)).isEqualTo(Solution(
                        newLocalEvents = localEvents,
                        newRemoteEvents = remoteEvents + listOf(event)
                ))
                assertThat(strategy.merge(localEvents, remoteEvents, baseNote, baseNote, baseNote.apply(event).first)).isEqualTo(Solution(
                        newLocalEvents = localEvents + listOf(event),
                        newRemoteEvents = remoteEvents
                ))
                assertThat(strategy.merge(localEvents, remoteEvents, baseNote, baseNote.apply(event).first, baseNote.apply(event).first)).isEqualTo(Solution(
                        newLocalEvents = localEvents,
                        newRemoteEvents = remoteEvents
                ))
                for (conflictingEvent in conflictingEvents) {
                    assertThat(conflictingEvent.revision).isEqualTo(2)
                    assertThat(strategy.merge(localEvents, remoteEvents, baseNote, baseNote.apply(event).first, baseNote.apply(conflictingEvent).first)).isEqualTo(NoSolution)
                    assertThat(strategy.merge(localEvents, remoteEvents, baseNote, baseNote.apply(conflictingEvent).first, baseNote.apply(event).first)).isEqualTo(NoSolution)
                }
                for (nonConflictingEvent in nonConflictingEvents) {
                    assertThat(nonConflictingEvent.revision).isEqualTo(2)
                    assertThat(strategy.merge(localEvents, remoteEvents, baseNote, baseNote.apply(event).first, baseNote.apply(nonConflictingEvent).first)).isEqualTo(Solution(
                            newLocalEvents = localEvents + listOf(nonConflictingEvent),
                            newRemoteEvents = remoteEvents + listOf(event)
                    ))
                    assertThat(strategy.merge(localEvents, remoteEvents, baseNote, baseNote.apply(nonConflictingEvent).first, baseNote.apply(event).first)).isEqualTo(Solution(
                            newLocalEvents = localEvents + listOf(event),
                            newRemoteEvents = remoteEvents + listOf(nonConflictingEvent)
                    ))
                }
            }
        }
    }

    private fun createStrategy() =
            SimpleMergeStrategy(differenceAnalyzer, differenceCompensator)
}