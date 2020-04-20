package info.maaskant.wmsnotes.client.synchronization.strategy.merge.note

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.model.note.TitleChangedEvent
import info.maaskant.wmsnotes.utilities.logger
import javax.inject.Inject

class KeepBothMergeStrategy @Inject constructor(
        private val differenceAnalyzer: DifferenceAnalyzer,
        private val differenceCompensator: DifferenceCompensator,
        private val aggregateIdGenerator: () -> String,
        private val conflictedNoteTitleSuffix: String
) : MergeStrategy<Note> {
    private val logger by logger()

    override fun merge(
            localEvents: List<Event>,
            remoteEvents: List<Event>,
            baseAggregate: Note,
            localAggregate: Note,
            remoteAggregate: Note
    ): MergeResult {
        val compensatingLocalEvents = getCompensatingLocalEvents(baseAggregate, localAggregate, remoteAggregate)
        val eventsForNewNote = getEventsForNewNote(localAggregate)
        logger.debug("Changing aggregate {} to {}, creating new note {} ", baseAggregate.aggId, remoteAggregate, localAggregate)
        return MergeResult.Solution(
                newLocalEvents = compensatingLocalEvents + eventsForNewNote,
                newRemoteEvents = eventsForNewNote
        )
    }

    private fun getCompensatingLocalEvents(baseNote: Note, localNote: Note, remoteNote: Note): List<Event> {
        val (compensatingLocalEvents, _) = differenceCompensator.compensate(
                aggId = localNote.aggId,
                differences = differenceAnalyzer.compare(left = localNote, right = remoteNote),
                target = DifferenceCompensator.Target.RIGHT
        )
        return compensatingLocalEvents
    }

    private fun getEventsForNewNote(localNote: Note): List<Event> {
        val newAggregateId = aggregateIdGenerator()
        val (compensatingEvents, _) = differenceCompensator.compensate(
                aggId = newAggregateId,
                differences = differenceAnalyzer.compare(left = Note(), right = localNote),
                target = DifferenceCompensator.Target.RIGHT
        )
        val titleChangedEvent = TitleChangedEvent(
                eventId = 0,
                aggId = newAggregateId,
                revision = compensatingEvents.last().revision + 1,
                title = localNote.title + conflictedNoteTitleSuffix
        )
        return compensatingEvents + titleChangedEvent
    }
}