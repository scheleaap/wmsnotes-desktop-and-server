package info.maaskant.wmsnotes.client.synchronization.strategy.merge.note

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.note.ExistenceDifference.ExistenceType.*
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.note.*

class DifferenceCompensator {
    private val defaultPath = Path()
    private val defaultContent = ""

    fun compensate(aggId: String, differences: Set<Difference>, target: Target): CompensatingEvents {
        return differences
                .flatMap { createEventsForDifference(aggId, target, it) }
                .sortedBy {
                    when (it) {
                        is NoteCreatedEvent -> -2
                        is NoteDeletedEvent -> 1
                        is NoteUndeletedEvent -> -1
                        else -> 0
                    }
                }
                .let {
                    when (target) {
                        Target.LEFT -> CompensatingEvents(
                                leftEvents = emptyList(),
                                rightEvents = it
                        )
                        Target.RIGHT -> CompensatingEvents(
                                leftEvents = it,
                                rightEvents = emptyList()
                        )
                    }
                }
    }

    private fun createEventsForDifference(aggId: String, target: Target, difference: Difference): List<Event> {
        return when (difference) {
            is ExistenceDifference -> eventsForExistenceDifference(aggId, difference, target)
            is PathDifference -> eventsForPathDifference(aggId, difference, target)
            is TitleDifference -> eventsForTitleDifference(aggId, difference, target)
            is ContentDifference -> eventsForContentDifference(aggId, difference, target)
            is AttachmentDifference -> eventsForAttachmentDifference(aggId, difference, target)
        }
    }

    private fun eventsForExistenceDifference(aggId: String, difference: ExistenceDifference, target: Target): List<Event> {
        return when (target) {
            Target.LEFT ->
                when (difference.left) {
                    NOT_YET_CREATED -> throw IllegalArgumentException()
                    EXISTS -> when (difference.right) {
                        NOT_YET_CREATED -> listOf(NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 0, path = defaultPath, title = aggId, content = defaultContent))
                        EXISTS -> throw IllegalArgumentException()
                        DELETED -> listOf(NoteUndeletedEvent(eventId = 0, aggId = aggId, revision = 0))
                    }
                    DELETED -> when (difference.right) {
                        NOT_YET_CREATED -> listOf(
                                NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 0, path = defaultPath, title = aggId, content = defaultContent),
                                NoteDeletedEvent(eventId = 0, aggId = aggId, revision = 0)
                        )
                        EXISTS -> listOf(NoteDeletedEvent(eventId = 0, aggId = aggId, revision = 0))
                        DELETED -> throw IllegalArgumentException()
                    }
                }
            Target.RIGHT -> when (difference.right) {
                NOT_YET_CREATED -> throw IllegalArgumentException()
                EXISTS -> when (difference.left) {
                    NOT_YET_CREATED -> listOf(NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 0, path = defaultPath, title = aggId, content = defaultContent))
                    EXISTS -> throw IllegalArgumentException()
                    DELETED -> listOf(NoteUndeletedEvent(eventId = 0, aggId = aggId, revision = 0))
                }
                DELETED -> when (difference.left) {
                    NOT_YET_CREATED -> listOf(
                            NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 0, path = defaultPath, title = aggId, content = defaultContent),
                            NoteDeletedEvent(eventId = 0, aggId = aggId, revision = 0)
                    )
                    EXISTS -> listOf(NoteDeletedEvent(eventId = 0, aggId = aggId, revision = 0))
                    DELETED -> throw IllegalArgumentException()
                }
            }
        }
    }

    private fun eventsForAttachmentDifference(aggId: String, difference: AttachmentDifference, target: Target): List<Event> =
            if (target == Target.LEFT && difference.left != null) {
                if (difference.right != null) {
                    listOf(AttachmentDeletedEvent(eventId = 0, aggId = aggId, revision = 0, name = difference.name))
                } else {
                    emptyList()
                } + AttachmentAddedEvent(eventId = 0, aggId = aggId, revision = 0, name = difference.name, content = difference.left)
            } else if (target == Target.RIGHT && difference.right != null) {
                if (difference.left != null) {
                    listOf(AttachmentDeletedEvent(eventId = 0, aggId = aggId, revision = 0, name = difference.name))
                } else {
                    emptyList()
                } + AttachmentAddedEvent(eventId = 0, aggId = aggId, revision = 0, name = difference.name, content = difference.right)
            } else {
                listOf(AttachmentDeletedEvent(eventId = 0, aggId = aggId, revision = 0, name = difference.name))
            }

    private fun eventsForContentDifference(aggId: String, difference: ContentDifference, target: Target): List<ContentChangedEvent> =
            listOf(ContentChangedEvent(eventId = 0, aggId = aggId, revision = 0, content = when (target) {
                Target.LEFT -> difference.left
                Target.RIGHT -> difference.right
            }))

    private fun eventsForPathDifference(aggId: String, difference: PathDifference, target: Target): List<MovedEvent> =
            listOf(MovedEvent(eventId = 0, aggId = aggId, revision = 0, path = when (target) {
                Target.LEFT -> difference.left
                Target.RIGHT -> difference.right
            }))

    private fun eventsForTitleDifference(aggId: String, difference: TitleDifference, target: Target): List<TitleChangedEvent> =
            listOf(TitleChangedEvent(eventId = 0, aggId = aggId, revision = 0, title = when (target) {
                Target.LEFT -> difference.left
                Target.RIGHT -> difference.right
            }))

    enum class Target {
        LEFT,
        RIGHT
    }

    data class CompensatingEvents(val leftEvents: List<Event>, val rightEvents: List<Event>)
}
