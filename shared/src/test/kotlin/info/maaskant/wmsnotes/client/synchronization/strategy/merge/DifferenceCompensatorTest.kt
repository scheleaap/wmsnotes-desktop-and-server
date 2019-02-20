package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.ExistenceDifference.ExistenceType.*
import info.maaskant.wmsnotes.model.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.*

@Disabled("Tests written while traveling, code to be implemented next")
internal class DifferenceCompensatorTest {
    private val noteId = "note"
    private val attachmentName = "att"
    private val attachmentContent1 = "data".toByteArray()
    private val attachmentContent2 = "different".toByteArray()

    @TestFactory
    fun existence(): List<DynamicTest> {
        val noteCreatedEvent = NoteCreatedEvent(eventId = 0, noteId = UUID.randomUUID().toString() /* TODO: should be random */, revision = 0, title = noteId)
        val noteDeletedEvent = NoteDeletedEvent(eventId = 0, noteId = noteId, revision = 0)
        val noteUndeletedEvent = NoteUndeletedEvent(eventId = 0, noteId = noteId, revision = 0)
        val items = listOf(
                Triple(
                        ExistenceDifference(EXISTS, DELETED),
                        DifferenceCompensator.Target.LEFT,
                        listOf(noteUndeletedEvent)
                ),
                Triple(
                        ExistenceDifference(EXISTS, DELETED),
                        DifferenceCompensator.Target.RIGHT,
                        listOf(noteDeletedEvent)
                ),
                Triple(
                        ExistenceDifference(DELETED, EXISTS),
                        DifferenceCompensator.Target.LEFT,
                        listOf(noteDeletedEvent)
                ),
                Triple(
                        ExistenceDifference(DELETED, EXISTS),
                        DifferenceCompensator.Target.RIGHT,
                        listOf(noteUndeletedEvent)
                ),
                Triple(
                        ExistenceDifference(NOT_YET_CREATED, EXISTS),
                        DifferenceCompensator.Target.RIGHT,
                        listOf(noteCreatedEvent) // TODO: This noteId should be random
                ),
                Triple(
                        ExistenceDifference(NOT_YET_CREATED, DELETED),
                        DifferenceCompensator.Target.RIGHT,
                        listOf(noteCreatedEvent, noteDeletedEvent)
                )
        )
        return items.map { (difference, target, compensatingEvents) ->
            DynamicTest.dynamicTest("$difference, $target chosen") {
                // Given
                val compensator = DifferenceCompensator()

                // When
                val events = compensator.compensate(noteId = noteId, differences = setOf(difference), target = target)

                // Then
                assertThat(events).isEqualTo(DifferenceCompensator.CompensatingEvents(
                        leftEvents = if (target == DifferenceCompensator.Target.LEFT) emptyList() else compensatingEvents,
                        rightEvents = if (target == DifferenceCompensator.Target.RIGHT) emptyList() else compensatingEvents
                ))
            }
        }
    }

    @TestFactory
    fun `other differences`(): List<DynamicTest> {
        val attachmentAddedEvent = AttachmentAddedEvent(eventId = 0, noteId = noteId, revision = 0, name = attachmentName, content = attachmentContent1)
        val attachmentDeletedEvent = AttachmentDeletedEvent(eventId = 0, noteId = noteId, revision = 0, name = attachmentName)
        val items = listOf(
                Triple(
                        ContentDifference("left", "right"),
                        DifferenceCompensator.Target.LEFT,
                        listOf(ContentChangedEvent(eventId = 0, noteId = noteId, revision = 0, content = "left"))
                ),
                Triple(
                        ContentDifference("left", "right"),
                        DifferenceCompensator.Target.RIGHT,
                        listOf(ContentChangedEvent(eventId = 0, noteId = noteId, revision = 0, content = "right"))
                ),
                Triple(
                        TitleDifference("left", "right"),
                        DifferenceCompensator.Target.LEFT,
                        listOf(TitleChangedEvent(eventId = 0, noteId = noteId, revision = 0, title = "left"))
                ),
                Triple(
                        TitleDifference("left", "right"),
                        DifferenceCompensator.Target.RIGHT,
                        listOf(TitleChangedEvent(eventId = 0, noteId = noteId, revision = 0, title = "right"))
                ),
                Triple(
                        AttachmentDifference(attachmentName, attachmentContent1, null),
                        DifferenceCompensator.Target.LEFT,
                        listOf(attachmentAddedEvent)
                ),
                Triple(
                        AttachmentDifference(attachmentName, attachmentContent1, null),
                        DifferenceCompensator.Target.RIGHT,
                        listOf(attachmentDeletedEvent)
                ),
                Triple(
                        AttachmentDifference(attachmentName, null, attachmentContent1),
                        DifferenceCompensator.Target.LEFT,
                        listOf(attachmentDeletedEvent)
                ),
                Triple(
                        AttachmentDifference(attachmentName, null, attachmentContent1),
                        DifferenceCompensator.Target.RIGHT,
                        listOf(attachmentAddedEvent)
                ),
                Triple(
                        AttachmentDifference(attachmentName, attachmentContent1, attachmentContent2),
                        DifferenceCompensator.Target.LEFT,
                        listOf(
                                AttachmentDeletedEvent(eventId = 0, noteId = noteId, revision = 0, name = attachmentName),
                                AttachmentAddedEvent(eventId = 0, noteId = noteId, revision = 0, name = attachmentName, content = attachmentContent1)
                        )
                ),
                Triple(
                        AttachmentDifference(attachmentName, attachmentContent1, attachmentContent2),
                        DifferenceCompensator.Target.RIGHT,
                        listOf(
                                AttachmentDeletedEvent(eventId = 0, noteId = noteId, revision = 0, name = attachmentName),
                                AttachmentAddedEvent(eventId = 0, noteId = noteId, revision = 0, name = attachmentName, content = attachmentContent2)
                        )
                )
        )
        return items.map { (difference, target, compensatingEvents) ->
            DynamicTest.dynamicTest("$difference, $target chosen") {
                // Given
                val compensator = DifferenceCompensator()

                // When
                val events = compensator.compensate(noteId = noteId, differences = setOf(difference), target = target)

                // Then
                assertThat(events).isEqualTo(DifferenceCompensator.CompensatingEvents(
                        leftEvents = if (target == DifferenceCompensator.Target.LEFT) emptyList() else compensatingEvents,
                        rightEvents = if (target == DifferenceCompensator.Target.RIGHT) emptyList() else compensatingEvents
                ))
            }
        }
    }

    @Test
    fun `existence event order`() {
        // Given
        val differences = setOf(ExistenceDifference(NOT_YET_CREATED, DELETED))
        val compensator = DifferenceCompensator()

        // When
        val events = compensator.compensate(noteId = noteId, differences = differences, target = DifferenceCompensator.Target.RIGHT)

        // Then
        val eventClasses = events.leftEvents.map { it::class }
        assertThat(eventClasses).isEqualTo(listOf(
                NoteCreatedEvent::class,
                NoteDeletedEvent::class
        ))
    }

    @Test
    fun `attachment event order`() {
        // Given
        val differences = setOf(AttachmentDifference(attachmentName, attachmentContent1, attachmentContent2))
        val compensator = DifferenceCompensator()

        // When
        val events = compensator.compensate(noteId = noteId, differences = differences, target = DifferenceCompensator.Target.RIGHT)

        // Then
        val eventClasses = events.leftEvents.map { it::class }
        assertThat(eventClasses).isEqualTo(listOf(
                AttachmentDeletedEvent::class,
                AttachmentAddedEvent::class
        ))
    }
}