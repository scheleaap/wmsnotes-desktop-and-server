package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Event

// TODO: Rename to CompensationStrategy or Compensator or something
interface SynchronizationStrategy {
    fun resolve(localEvents: List<Event>, remoteEvents: List<Event>): ResolutionResult

    sealed class ResolutionResult {
        object NoSolution : ResolutionResult()
        data class Solution(val compensatingActions: List<CompensatingAction>) : ResolutionResult() {
            constructor(compensatingAction: CompensatingAction) : this(listOf(compensatingAction))
        }
    }
}

