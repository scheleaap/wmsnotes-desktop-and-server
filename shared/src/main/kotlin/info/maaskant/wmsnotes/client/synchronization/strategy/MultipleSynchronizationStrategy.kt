package info.maaskant.wmsnotes.client.synchronization.strategy

import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult.NoSolution
import info.maaskant.wmsnotes.model.Event

class MultipleSynchronizationStrategy(vararg val strategies: SynchronizationStrategy) : SynchronizationStrategy {
    override fun resolve(noteId: String, localEvents: List<Event>, remoteEvents: List<Event>): SynchronizationStrategy.ResolutionResult {
        for (strategy in strategies) {
            val result = strategy.resolve(noteId, localEvents, remoteEvents)
            if (result != NoSolution) {
                return result
            }
        }
        return NoSolution
    }
}
