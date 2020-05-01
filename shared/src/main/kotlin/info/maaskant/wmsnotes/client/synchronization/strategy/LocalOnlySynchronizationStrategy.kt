package info.maaskant.wmsnotes.client.synchronization.strategy

import info.maaskant.wmsnotes.model.Event

class LocalOnlySynchronizationStrategy : SynchronizationStrategy {
    override fun resolve(aggId: String, localEvents: List<Event>, remoteEvents: List<Event>): SynchronizationStrategy.ResolutionResult =
            if (remoteEvents.isEmpty()) {
                SynchronizationStrategy.ResolutionResult.Solution(
                        compensatedLocalEvents = localEvents,
                        compensatedRemoteEvents = emptyList(),
                        newLocalEvents = emptyList(),
                        newRemoteEvents = localEvents
                )
            } else {
                SynchronizationStrategy.ResolutionResult.NoSolution
            }
}