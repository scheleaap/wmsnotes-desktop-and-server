package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import au.com.console.kassava.kotlinToString
import info.maaskant.wmsnotes.model.Aggregate
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.note.Note

interface MergeStrategy<AggregateType : Aggregate<AggregateType>> {
    fun merge(
            localEvents: List<Event>,
            remoteEvents: List<Event>,
            baseAggregate: AggregateType,
            localAggregate: AggregateType,
            remoteAggregate: AggregateType
    ): MergeResult

    sealed class MergeResult {
        object NoSolution : MergeResult() {
            override fun toString() = kotlinToString(properties = emptyArray())
        }

        data class Solution(
                val newLocalEvents: List<Event>,
                val newRemoteEvents: List<Event>
        ) : MergeResult()
    }
}
