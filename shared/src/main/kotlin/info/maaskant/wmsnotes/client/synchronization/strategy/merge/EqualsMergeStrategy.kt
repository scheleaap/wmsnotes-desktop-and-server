package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.utilities.logger

class EqualsMergeStrategy : MergeStrategy {
    private val logger by logger()

    override fun merge(
            localEvents: List<Event>,
            remoteEvents: List<Event>,
            baseNote: Note,
            localNote: Note,
            remoteNote: Note
    ): MergeResult {
        return if (localNote.equalsIgnoringRevision(remoteNote)) {
            logger.debug("{} and {} are equal", localNote, remoteNote)
            MergeResult.Solution(
                    newLocalEvents = emptyList(),
                    newRemoteEvents = emptyList()
            )
        } else {
            logger.debug("{} and {} are different", localNote, remoteNote)
            MergeResult.NoSolution
        }
    }
}