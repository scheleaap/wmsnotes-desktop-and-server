package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.note.Note

class EqualsMergeStrategy : MergeStrategy {
    override fun merge(
            localEvents: List<Event>,
            remoteEvents: List<Event>,
            baseNote: Note,
            localNote: Note,
            remoteNote: Note
    ): MergeResult {
        return if (localNote == remoteNote) {
            MergeResult.Solution(
                    newLocalEvents = emptyList(),
                    newRemoteEvents = emptyList()
            )
        } else {
            MergeResult.NoSolution
        }
    }
}