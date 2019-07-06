package info.maaskant.wmsnotes.client.synchronization.strategy.merge

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
) : MergeStrategy {
    private val logger by logger()

    override fun merge(
            localEvents: List<Event>,
            remoteEvents: List<Event>,
            baseNote: Note,
            localNote: Note,
            remoteNote: Note
    ): MergeResult {
        val compensatingLocalEvents = getCompensatingLocalEvents(baseNote, localNote, remoteNote)
        val eventsForNewNote = getEventsForNewNote(localNote)
        logger.debug("Changing aggregate {} to {}, creating new note {} ", baseNote.aggId, remoteNote, localNote)
        return MergeResult.Solution(
                newLocalEvents = compensatingLocalEvents + eventsForNewNote,
                newRemoteEvents = eventsForNewNote
        )
    }

    private fun getCompensatingLocalEvents(baseNote: Note, localNote: Note, remoteNote: Note): List<Event> {
        val (compensatingLocalEvents, _) = differenceCompensator.compensate(
                aggId = baseNote.aggId,
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