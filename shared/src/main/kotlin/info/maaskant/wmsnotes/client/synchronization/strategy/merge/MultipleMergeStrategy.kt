package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.NoSolution
import info.maaskant.wmsnotes.model.Aggregate
import info.maaskant.wmsnotes.model.Event

class MultipleMergeStrategy<AggregateType : Aggregate<AggregateType>>(vararg val strategies: MergeStrategy<AggregateType>) : MergeStrategy<AggregateType> {
    override fun merge(
            localEvents: List<Event>,
            remoteEvents: List<Event>,
            baseAggregate: AggregateType,
            localAggregate: AggregateType,
            remoteAggregate: AggregateType
    ): MergeStrategy.MergeResult {
        for (strategy in strategies) {
            val result = strategy.merge(localEvents, remoteEvents, baseAggregate, localAggregate, remoteAggregate)
            if (result != NoSolution) {
                return result
            }
        }
        return NoSolution
    }
}
