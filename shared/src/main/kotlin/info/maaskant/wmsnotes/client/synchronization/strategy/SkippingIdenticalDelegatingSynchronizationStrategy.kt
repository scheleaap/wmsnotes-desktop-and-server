package info.maaskant.wmsnotes.client.synchronization.strategy

import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult.Solution
import info.maaskant.wmsnotes.model.Event
import kotlin.math.min

class SkippingIdenticalDelegatingSynchronizationStrategy(private val delegate: SynchronizationStrategy) : SynchronizationStrategy {
    override fun resolve(aggId: String, localEvents: List<Event>, remoteEvents: List<Event>): ResolutionResult {
        val indexOfFirstDifferent = (localEvents zip remoteEvents)
                .indexOfFirst { (localEvent, remoteEvent) ->
                    localEvent != remoteEvent.copy(eventId = localEvent.eventId, revision = localEvent.revision)
                }
                .let { if (it == -1) min(localEvents.size, remoteEvents.size) else it }
        val filteredLocalEvents = localEvents.drop(indexOfFirstDifferent)
        val filteredRemoteEvents = remoteEvents.drop(indexOfFirstDifferent)
        return if (filteredLocalEvents.isNotEmpty() || filteredRemoteEvents.isNotEmpty()) {
            @Suppress("MoveVariableDeclarationIntoWhen")
            val delegateResult = delegate.resolve(aggId = aggId, localEvents = filteredLocalEvents, remoteEvents = filteredRemoteEvents)
            when (delegateResult) {
                NoSolution -> delegateResult
                is Solution -> Solution(
                        compensatedLocalEvents = localEvents,
                        compensatedRemoteEvents = remoteEvents,
                        newLocalEvents = delegateResult.newLocalEvents,
                        newRemoteEvents = delegateResult.newRemoteEvents
                )
            }
        } else {
            Solution(
                    compensatedLocalEvents = localEvents,
                    compensatedRemoteEvents = remoteEvents,
                    newLocalEvents = emptyList(),
                    newRemoteEvents = emptyList()
            )
        }
    }
}
