package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.model.Event

class DifferenceCompensator {
    fun compensate(noteId: String, differences: Set<Difference>, target: Target): CompensatingEvents {
        TODO()
    }

    enum class Target {
        LEFT,
        RIGHT
    }

    data class CompensatingEvents(val leftEvents: List<Event>, val rightEvents: List<Event>)
}
