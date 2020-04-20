package info.maaskant.wmsnotes.testutilities

import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinToString
import info.maaskant.wmsnotes.model.Aggregate
import info.maaskant.wmsnotes.model.Event
import java.util.*

class Toastie constructor(
        override val revision: Int,
        override val aggId: String,
        val value: Int
) : Aggregate<Toastie> {

    override fun equals(other: Any?) = kotlinEquals(other = other, properties = arrayOf(Toastie::aggId, Toastie::revision, Toastie::value))
    override fun equalsIgnoringRevision(other: Any?) = kotlinEquals(other = other, properties = arrayOf(Toastie::aggId, Toastie::value))
    override fun hashCode() = Objects.hash(aggId, revision, value)
    override fun toString() = kotlinToString(properties = arrayOf(Toastie::aggId, Toastie::revision, Toastie::value))

    override fun apply(event: Event): Pair<Toastie, Event?> {
        val expectedRevision = revision + 1
        if (event.aggId != aggId) throw IllegalArgumentException("The aggregate id of $event must be $aggId")
        if (event.revision != expectedRevision) throw IllegalArgumentException("The revision of $event must be $expectedRevision")
//        return when (event) {
//            is ToastieEvent -> when (event) {
//                is ToastieIncreaseValueEvent -> Toastie(
//                        revision = event.revision,
//                        aggId = aggId,
//                        value = value + 1
//                ) to event
//            }
//            else -> throw IllegalArgumentException("Wrong event type $event")
//        }
        TODO()
    }
}