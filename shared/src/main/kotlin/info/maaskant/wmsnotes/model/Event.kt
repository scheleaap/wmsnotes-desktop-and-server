package info.maaskant.wmsnotes.model

import au.com.console.kassava.SupportsMixedTypeEquality
import au.com.console.kassava.kotlinEquals
import java.util.*

abstract class Event(val eventId: Int, val aggId: String, val revision: Int) : SupportsMixedTypeEquality {
    abstract fun copy(eventId: Int = this.eventId, revision: Int = this.revision): Event

    override fun equals(other: Any?) = kotlinEquals(other = other, properties = arrayOf(Event::eventId, Event::aggId, Event::revision))
    override fun hashCode() = Objects.hash(eventId, aggId, revision)
}