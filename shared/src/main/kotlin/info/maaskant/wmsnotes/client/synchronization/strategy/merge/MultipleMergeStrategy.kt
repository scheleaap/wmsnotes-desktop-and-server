package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.NoSolution
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.note.Note

class MultipleMergeStrategy(vararg val strategies: MergeStrategy) : MergeStrategy {
    override fun merge(localEvents: List<Event>, remoteEvents: List<Event>, baseNote: Note, localNote: Note, remoteNote: Note): MergeStrategy.MergeResult {
        for (strategy in strategies) {
            val result = strategy.merge(localEvents, remoteEvents, baseNote, localNote, remoteNote)
            if (result != NoSolution) {
                return result
            }
        }
        return NoSolution
    }
}
