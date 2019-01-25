package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Event

class LocalOnlySynchronizationStrategy : SynchronizationStrategy {
    override fun resolve(localEvents: List<Event>, remoteEvents: List<Event>): SynchronizationStrategy.ResolutionResult =
            if (remoteEvents.isEmpty()) {
                SynchronizationStrategy.ResolutionResult.Solution(
                        localEvents.map {
                            CompensatingAction(
                                    compensatedLocalEvents = listOf(it),
                                    compensatedRemoteEvents = emptyList(),
                                    newLocalEvents = emptyList(),
                                    newRemoteEvents = listOf(it)
                            )
                        }
                )
            } else {
                SynchronizationStrategy.ResolutionResult.NoSolution
            }
}