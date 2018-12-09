package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.synchronization.SynchronizationStrategy.ResolutionResult.NoSolution
import info.maaskant.wmsnotes.model.Event

class MultipleSynchronizationStrategy(vararg val strategies: SynchronizationStrategy) : SynchronizationStrategy {
    override fun resolve(localEvents: List<Event>, remoteEvents: List<Event>): SynchronizationStrategy.ResolutionResult {
        for (strategy in strategies) {
            val result = strategy.resolve(localEvents, remoteEvents)
            if (result != NoSolution) {
                return result
            }
        }
        return NoSolution
    }
}
