package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class DifferenceCompensatorTest {
    private val noteId = "note"
    private val attachmentName = "att"
    private val attachmentContent = "data".toByteArray()

    @TestFactory
    fun existence(): List<DynamicTest> {
        val noteCreatedEvent = NoteCreatedEvent(eventId = 0, noteId = noteId, revision = 0, title = noteId)
        val noteDeletedEvent = NoteDeletedEvent(eventId = 0, noteId = noteId, revision = 0)
        val attachmentAddedEvent = AttachmentAddedEvent(eventId = 0, noteId = noteId, revision = 0, name = attachmentName, content = attachmentContent)
        val attachmentDeletedEvent = AttachmentDeletedEvent(eventId = 0, noteId = noteId, revision = 0, name = attachmentName)
        val items = listOf(
                Triple(
                        ExistenceDifference(true, false),
                        DifferenceCompensator.Target.LEFT,
                        noteCreatedEvent
                ),
                Triple(
                        ExistenceDifference(true, false),
                        DifferenceCompensator.Target.RIGHT,
                        noteDeletedEvent
                ),
                Triple(
                        ExistenceDifference(false, true),
                        DifferenceCompensator.Target.LEFT,
                        noteDeletedEvent
                ),
                Triple(
                        ExistenceDifference(false, true),
                        DifferenceCompensator.Target.RIGHT,
                        noteCreatedEvent
                ),
                Triple(
                        ContentDifference("left", "right"),
                        DifferenceCompensator.Target.LEFT,
                        ContentChangedEvent(eventId = 0, noteId = noteId, revision = 0, content = "left")
                ),
                Triple(
                        ContentDifference("left", "right"),
                        DifferenceCompensator.Target.RIGHT,
                        ContentChangedEvent(eventId = 0, noteId = noteId, revision = 0, content = "right")
                ),
                Triple(
                        AttachmentDifference(attachmentName, attachmentContent, null),
                        DifferenceCompensator.Target.LEFT,
                        attachmentAddedEvent
                ),
                Triple(
                        AttachmentDifference(attachmentName, attachmentContent, null),
                        DifferenceCompensator.Target.RIGHT,
                        attachmentDeletedEvent
                ),
                Triple(
                        AttachmentDifference(attachmentName, null, attachmentContent),
                        DifferenceCompensator.Target.LEFT,
                        attachmentDeletedEvent
                ),
                Triple(
                        AttachmentDifference(attachmentName, null, attachmentContent),
                        DifferenceCompensator.Target.RIGHT,
                        attachmentAddedEvent
                )
        )
        return items.map { (difference, target, compensatingEvent) ->
            DynamicTest.dynamicTest("$difference, $target chosen") {
                val compensator = DifferenceCompensator()

                // When
                val events = compensator.compensate(differences = setOf(difference), target = target)

                // Then
                assertThat(events).isEqualTo(DifferenceCompensator.CompensatingEvents(
                        leftEvents = if (target == DifferenceCompensator.Target.LEFT) listOf(compensatingEvent) else emptyList(),
                        rightEvents = if (target == DifferenceCompensator.Target.RIGHT) listOf(compensatingEvent) else emptyList()
                ))
            }
        }
    }

    @Test
    fun `attachment changed, left chosen`() {
        // Given
        val differentContent = "different".toByteArray()
        val difference = AttachmentDifference(attachmentName, attachmentContent, differentContent)
        val compensator = DifferenceCompensator()

        // When
        val events = compensator.compensate(differences = setOf(difference), target = DifferenceCompensator.Target.LEFT)

        // Then
        assertThat(events).isEqualTo(DifferenceCompensator.CompensatingEvents(leftEvents = emptyList(), rightEvents = listOf(
                AttachmentDeletedEvent(eventId = 0, noteId = noteId, revision = 0, name = attachmentName),
                AttachmentAddedEvent(eventId = 0, noteId = noteId, revision = 0, name = attachmentName, content = attachmentContent)
        )))
    }

    @Test
    fun `attachment changed, right chosen`() {
        // Given
        val differentContent = "different".toByteArray()
        val difference = AttachmentDifference(attachmentName, attachmentContent, differentContent)
        val compensator = DifferenceCompensator()

        // When
        val events = compensator.compensate(differences = setOf(difference), target = DifferenceCompensator.Target.RIGHT)

        // Then
        assertThat(events).isEqualTo(DifferenceCompensator.CompensatingEvents(leftEvents = listOf(
                AttachmentDeletedEvent(eventId = 0, noteId = noteId, revision = 0, name = attachmentName),
                AttachmentAddedEvent(eventId = 0, noteId = noteId, revision = 0, name = attachmentName, content = differentContent)
        ), rightEvents = emptyList()))
    }

    @Test
    fun `NoteCreatedEvent order`() {
        // Given
        val differences = setOf(
                ExistenceDifference(true, false),
                ContentDifference("left", "right"),
                AttachmentDifference(attachmentName, attachmentContent, null)
        )
        val compensator = DifferenceCompensator()

        // When
        val events = compensator.compensate(differences = differences, target = DifferenceCompensator.Target.RIGHT)

        // Then
        val eventClasses = events.leftEvents.map { it::class }
        assertThat(eventClasses).isEqualTo(listOf(
                NoteCreatedEvent::class,
                ContentChangedEvent::class,
                AttachmentDeletedEvent::class
        ))
    }

    @Test
    fun `NoteDeletedEvent order`() {
        // Given
        val differences = setOf(
                ExistenceDifference(true, false),
                ContentDifference("left", "right"),
                AttachmentDifference(attachmentName, attachmentContent, null)
        )
        val compensator = DifferenceCompensator()

        // When
        val events = compensator.compensate(differences = differences, target = DifferenceCompensator.Target.LEFT)

        // Then
        val eventClasses = events.rightEvents.map { it::class }
        assertThat(eventClasses).isEqualTo(listOf(
                ContentChangedEvent::class,
                AttachmentDeletedEvent::class,
                NoteDeletedEvent::class
        ))
    }
}