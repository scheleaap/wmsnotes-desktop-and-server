package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.CompensatingAction
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy
import info.maaskant.wmsnotes.model.Aggregate
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.utilities.logger

// TODO: Prevent merging folders
class MergingSynchronizationStrategy(
        private val mergeStrategy: MergeStrategy,
        private val noteRepository: AggregateRepository<Note>
) : SynchronizationStrategy {
    private val logger by logger()

    override fun resolve(aggId: String, localEvents: List<Event>, remoteEvents: List<Event>): SynchronizationStrategy.ResolutionResult {
        return if (localEvents.isEmpty() || remoteEvents.isEmpty()) {
            logger.warn("Unexpected call to ${this.javaClass.name} with $localEvents and $remoteEvents")
            SynchronizationStrategy.ResolutionResult.NoSolution
        } else {
            val baseRevision = localEvents.first().revision - 1
            val modifiedRemoteEvents = remoteEvents
                    .zip((baseRevision + 1)..(baseRevision + remoteEvents.size))
                    .map { (event, revision) -> event.copy(revision = revision) }
            val baseNote = noteRepository.get(aggId = aggId, revision = baseRevision)
            val localNote = Aggregate.apply(baseNote, localEvents)
            val remoteNote = Aggregate.apply(baseNote, modifiedRemoteEvents)
            val mergeResult = mergeStrategy.merge(
                    localEvents = localEvents,
                    remoteEvents = remoteEvents,
                    baseNote = baseNote,
                    localNote = localNote,
                    remoteNote = remoteNote
            )
            return when (mergeResult) {
                MergeStrategy.MergeResult.NoSolution -> SynchronizationStrategy.ResolutionResult.NoSolution
                is MergeStrategy.MergeResult.Solution -> {
                    SynchronizationStrategy.ResolutionResult.Solution(
                            compensatingAction = CompensatingAction(
                                    compensatedLocalEvents = localEvents,
                                    compensatedRemoteEvents = remoteEvents,
                                    newLocalEvents = mergeResult.newLocalEvents,
                                    newRemoteEvents = mergeResult.newRemoteEvents
                            )
                    )
                }
            }
        }
    }
}