package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.ExistenceDifference.ExistenceType.*
import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.projection.Note
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class DifferenceCompensatorTest {
    private val noteId = "note"
    private val attachmentName = "att"
    private val attachmentContent1 = "data".toByteArray()
    private val attachmentContent2 = "different".toByteArray()

    @TestFactory
    fun existence(): List<DynamicTest> {
        val noteCreatedEvent = NoteCreatedEvent(eventId = 0, noteId = noteId, revision = 0, path = Path(), title = noteId, content = "")
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
                        ExistenceDifference(EXISTS, NOT_YET_CREATED),
                        DifferenceCompensator.Target.LEFT,
                        listOf(noteCreatedEvent)
                ),
                Triple(
                        ExistenceDifference(DELETED, NOT_YET_CREATED),
                        DifferenceCompensator.Target.LEFT,
                        listOf(noteCreatedEvent, noteDeletedEvent)
                ),
                Triple(
                        ExistenceDifference(NOT_YET_CREATED, EXISTS),
                        DifferenceCompensator.Target.RIGHT,
                        listOf(noteCreatedEvent)
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
                        PathDifference(Path("left"), Path("right")),
                        DifferenceCompensator.Target.LEFT,
                        listOf(MovedEvent(eventId = 0, noteId = noteId, revision = 0, path = Path("left")))
                ),
                Triple(
                        PathDifference(Path("left"), Path("right")),
                        DifferenceCompensator.Target.RIGHT,
                        listOf(MovedEvent(eventId = 0, noteId = noteId, revision = 0, path = Path("right")))
                ),
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

    @Test
    fun `existence and other events order, 1`() {
        // Given
        val differences = setOf(
                ExistenceDifference(DELETED, NOT_YET_CREATED),
                TitleDifference("left", "right")
        )
        val compensator = DifferenceCompensator()

        // When
        val events = compensator.compensate(noteId = noteId, differences = differences, target = DifferenceCompensator.Target.LEFT)

        // Then
        val eventClasses = events.rightEvents.map { it::class }
        assertThat(eventClasses).isEqualTo(listOf(
                NoteCreatedEvent::class,
                TitleChangedEvent::class,
                NoteDeletedEvent::class
        ))
    }

    @Test
    fun `existence and other events order, 2`() {
        // Given
        val differences = setOf(
                ExistenceDifference(EXISTS, DELETED),
                TitleDifference("left", "right")
        )
        val compensator = DifferenceCompensator()

        // When
        val events = compensator.compensate(noteId = noteId, differences = differences, target = DifferenceCompensator.Target.LEFT)

        // Then
        val eventClasses = events.rightEvents.map { it::class }
        assertThat(eventClasses).isEqualTo(listOf(
                NoteUndeletedEvent::class,
                TitleChangedEvent::class
        ))
    }

    @Test
    fun `real-world case 1`() {
        // Given
        val differences = setOf(
                ExistenceDifference(NOT_YET_CREATED, EXISTS),
                ContentDifference("", "text"),
                TitleDifference("", "title")
        )
        val compensator = DifferenceCompensator()

        // When
        val events = compensator.compensate(noteId = noteId, differences = differences, target = DifferenceCompensator.Target.RIGHT)

        // Then
        assertThat(events.leftEvents).isEqualTo(listOf(
                NoteCreatedEvent(eventId = 0, noteId = noteId, revision = 0, path = Path(), title = noteId, content = ""),
                ContentChangedEvent(eventId = 0, noteId = noteId, revision = 0, content = "text"),
                TitleChangedEvent(eventId = 0, noteId = noteId, revision = 0, title = "title")
        ))
    }
}