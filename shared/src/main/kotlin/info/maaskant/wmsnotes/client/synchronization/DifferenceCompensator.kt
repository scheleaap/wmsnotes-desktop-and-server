package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Event

class DifferenceCompensator {
    fun compensate(differences: Set<Difference>, target: Target): Set<CompensatingAction> {
        TODO()
    }

    enum class Target {
        LEFT,
        RIGHT
    }

    data class CompensatingAction(val leftEvents: List<Event>, val rightEvents: List<Event>)
}
