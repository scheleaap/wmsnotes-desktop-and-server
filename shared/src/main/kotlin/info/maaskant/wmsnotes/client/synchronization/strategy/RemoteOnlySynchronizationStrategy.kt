package info.maaskant.wmsnotes.client.synchronization.strategy

import info.maaskant.wmsnotes.model.Event

class RemoteOnlySynchronizationStrategy : SynchronizationStrategy {
    override fun resolve(aggId: String, localEvents: List<Event>, remoteEvents: List<Event>): SynchronizationStrategy.ResolutionResult =
            if (localEvents.isEmpty()) {
                SynchronizationStrategy.ResolutionResult.Solution(
                        compensatedLocalEvents = emptyList(),
                        compensatedRemoteEvents = remoteEvents,
                        newLocalEvents = remoteEvents,
                        newRemoteEvents = emptyList()
                )
            } else {
                SynchronizationStrategy.ResolutionResult.NoSolution
            }
}