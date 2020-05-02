package info.maaskant.wmsnotes.client.synchronization.strategy

import au.com.console.kassava.kotlinToString
import info.maaskant.wmsnotes.model.Event

/**
 * A strategy to compensate for unsynchronized local and remote events. Typically, unsynchronized local events are
 * compensated by producing new remote events and unsynchronized remote events by new local events. This is not a
 * requirement however, and implementations may choose different ways to compensate. Compensation is done on a per-note
 * basis.
 */
interface SynchronizationStrategy {
    /**
     * Attempts to find a solution that compensates for a set of local and remote events.
     *
     * @param aggId The id of the note all events belong to.
     * @return A [ResolutionResult.Solution] or [ResolutionResult.NoSolution]
     */
    fun resolve(aggId: String, localEvents: List<Event>, remoteEvents: List<Event>): ResolutionResult

    sealed class ResolutionResult {
        object NoSolution : ResolutionResult() {
            override fun toString() = kotlinToString(properties = emptyArray())
        }

        data class Solution(
                val compensatedLocalEvents: List<Event>,
                val compensatedRemoteEvents: List<Event>,
                val newLocalEvents: List<Event>,
                val newRemoteEvents: List<Event>
        ) : ResolutionResult()
    }
}

