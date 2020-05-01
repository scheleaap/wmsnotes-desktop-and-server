package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import arrow.core.extensions.list.foldable.firstOption
import arrow.core.getOrElse
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy
import info.maaskant.wmsnotes.model.Aggregate
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.utilities.logger

abstract class MergingSynchronizationStrategy<AggregateType : Aggregate<AggregateType>>(
        private val mergeStrategy: MergeStrategy<AggregateType>,
        private val aggregateRepository: AggregateRepository<AggregateType>
) : SynchronizationStrategy {
    private val logger by logger()

    abstract fun canHandleEvent(it: Event): Boolean

    override fun resolve(aggId: String, localEvents: List<Event>, remoteEvents: List<Event>): SynchronizationStrategy.ResolutionResult {
        return if (localEvents.isEmpty() || remoteEvents.isEmpty()) {
            logger.warn("Unexpected call to ${this.javaClass.name} with $localEvents and $remoteEvents")
            SynchronizationStrategy.ResolutionResult.NoSolution
        } else if (!(localEvents.firstOption().map(this::canHandleEvent).getOrElse { true } &&
                        remoteEvents.firstOption().map(this::canHandleEvent).getOrElse { true })) {
            SynchronizationStrategy.ResolutionResult.NoSolution
        } else {
            return resolveInternal(aggId, localEvents, remoteEvents)
        }
    }

    private fun resolveInternal(aggId: String, localEvents: List<Event>, remoteEvents: List<Event>): SynchronizationStrategy.ResolutionResult {
        val baseRevision = localEvents.first().revision - 1
        val modifiedRemoteEvents = remoteEvents
                .zip((baseRevision + 1)..(baseRevision + remoteEvents.size))
                .map { (event, revision) -> event.copy(revision = revision) }
        val baseAggregate = aggregateRepository.get(aggId = aggId, revision = baseRevision)
        val localAggregate = Aggregate.apply(baseAggregate, localEvents)
        val remoteAggregate = Aggregate.apply(baseAggregate, modifiedRemoteEvents)
        logger.debug("Attempting to merge events for aggregate {} with base {}, local {} and remote {}", aggId, baseAggregate, localAggregate, remoteAggregate)
        val mergeResult = mergeStrategy.merge(
                localEvents = localEvents,
                remoteEvents = remoteEvents,
                baseAggregate = baseAggregate,
                localAggregate = localAggregate,
                remoteAggregate = remoteAggregate
        )
        return when (mergeResult) {
            MergeStrategy.MergeResult.NoSolution -> SynchronizationStrategy.ResolutionResult.NoSolution
            is MergeStrategy.MergeResult.Solution -> {
                SynchronizationStrategy.ResolutionResult.Solution(
                        compensatedLocalEvents = localEvents,
                        compensatedRemoteEvents = remoteEvents,
                        newLocalEvents = mergeResult.newLocalEvents,
                        newRemoteEvents = mergeResult.newRemoteEvents
                )
            }
        }
    }
}