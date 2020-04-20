package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult
import info.maaskant.wmsnotes.model.Aggregate
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.utilities.logger

class EqualsMergeStrategy<AggregateType : Aggregate<AggregateType>> : MergeStrategy<AggregateType> {
    private val logger by logger()

    override fun merge(
            localEvents: List<Event>,
            remoteEvents: List<Event>,
            baseAggregate: AggregateType,
            localAggregate: AggregateType,
            remoteAggregate: AggregateType
    ): MergeResult {
        return if (localAggregate.equalsIgnoringRevision(remoteAggregate)) {
            logger.debug("{} and {} are equal", localAggregate, remoteAggregate)
            MergeResult.Solution(
                    newLocalEvents = emptyList(),
                    newRemoteEvents = emptyList()
            )
        } else {
            logger.debug("{} and {} are different", localAggregate, remoteAggregate)
            MergeResult.NoSolution
        }
    }
}