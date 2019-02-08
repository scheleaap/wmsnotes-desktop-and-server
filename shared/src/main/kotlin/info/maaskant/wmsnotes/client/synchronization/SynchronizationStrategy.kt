package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Event

// TODO: Rename to CompensationStrategy or Compensator or something
/**
 * A strategy to compensate for unsynchronized local and remote events. Typically, unsynchronized local events are
 * compensated by producing new remote events and unsynchronized remote events by new local events. This is not a
 * requirement however, and implementations may choose different ways to compensate. Compensation is done on a per-note
 * basis.
 */
interface SynchronizationStrategy {
    /**
     * Attempts to find [CompensatingAction]s for a set of local and remote events.
     *
     * @param noteId The id of the note all events belong to.
     * @return A [ResolutionResult.Solution] or [ResolutionResult.NoSolution]
     */
    fun resolve(noteId: String, localEvents: List<Event>, remoteEvents: List<Event>): ResolutionResult

    sealed class ResolutionResult {
        object NoSolution : ResolutionResult()
        data class Solution(val compensatingActions: List<CompensatingAction>) : ResolutionResult() {
            constructor(compensatingAction: CompensatingAction) : this(listOf(compensatingAction))
        }
    }
}

