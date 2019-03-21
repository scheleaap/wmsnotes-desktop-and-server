package info.maaskant.wmsnotes.client.synchronization.strategy

import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult.NoSolution
import info.maaskant.wmsnotes.model.Event

class MultipleSynchronizationStrategy(vararg val strategies: SynchronizationStrategy) : SynchronizationStrategy {
    override fun resolve(aggId: String, localEvents: List<Event>, remoteEvents: List<Event>): SynchronizationStrategy.ResolutionResult {
        for (strategy in strategies) {
            val result = strategy.resolve(aggId, localEvents, remoteEvents)
            if (result != NoSolution) {
                return result
            }
        }
        return NoSolution
    }
}
