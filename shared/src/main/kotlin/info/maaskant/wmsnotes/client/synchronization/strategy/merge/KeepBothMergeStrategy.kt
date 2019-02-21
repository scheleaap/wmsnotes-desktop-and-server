package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.projection.Note
import java.util.*
import javax.inject.Inject

class KeepBothMergeStrategy @Inject constructor(
        private val differenceAnalyzer: DifferenceAnalyzer,
        private val differenceCompensator: DifferenceCompensator
) : MergeStrategy {
    override fun merge(
            localEvents: List<Event>,
            remoteEvents: List<Event>,
            baseNote: Note,
            localNote: Note,
            remoteNote: Note
    ): MergeResult {
        val (compensatingLocalEvents, _) = differenceCompensator.compensate(
                noteId = baseNote.noteId,
                differences = differenceAnalyzer.compare(left = localNote, right = remoteNote),
                target = DifferenceCompensator.Target.RIGHT
        )
        val newNoteId = UUID.randomUUID().toString()
        val (eventsForNewNote, _) = differenceCompensator.compensate(
                noteId = newNoteId,
                differences = differenceAnalyzer.compare(left = Note(), right = localNote),
                target = DifferenceCompensator.Target.RIGHT
        )
        return MergeResult.Solution(
                newLocalEvents = /*localEvents +*/ compensatingLocalEvents + eventsForNewNote,
                newRemoteEvents = /*remoteEvents +*/ eventsForNewNote
        )
    }
}