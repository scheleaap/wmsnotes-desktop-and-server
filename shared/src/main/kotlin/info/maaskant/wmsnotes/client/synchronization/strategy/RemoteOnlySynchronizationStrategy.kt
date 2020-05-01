package info.maaskant.wmsnotes.client.synchronization.strategy

import info.maaskant.wmsnotes.client.synchronization.CompensatingAction
import info.maaskant.wmsnotes.model.Event

class RemoteOnlySynchronizationStrategy : SynchronizationStrategy {
    override fun resolve(aggId: String, localEvents: List<Event>, remoteEvents: List<Event>): SynchronizationStrategy.ResolutionResult =
            if (localEvents.isEmpty()) {
                if (remoteEvents.isEmpty()) {
                    SynchronizationStrategy.ResolutionResult.Solution(emptyList())
                } else {
                    SynchronizationStrategy.ResolutionResult.Solution(CompensatingAction(
                            compensatedLocalEvents = emptyList(),
                            compensatedRemoteEvents = remoteEvents,
                            newLocalEvents = remoteEvents,
                            newRemoteEvents = emptyList()
                    ))
                }
            } else {
                SynchronizationStrategy.ResolutionResult.NoSolution
            }
}