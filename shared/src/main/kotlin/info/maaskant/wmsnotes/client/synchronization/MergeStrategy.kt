package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.projection.Note

interface MergeStrategy {
    fun merge(
            localEvents: List<Event>,
            remoteEvents: List<Event>,
            baseNote: Note,
            localNote: Note,
            remoteNote: Note
    ): MergeResult

    sealed class MergeResult {
        object NoSolution : MergeResult()
        data class Solution(
                val newLocalEvents: List<Event>,
                val newRemoteEvents: List<Event>
        ) : MergeResult()
    }
}
