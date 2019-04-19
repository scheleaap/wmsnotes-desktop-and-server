package info.maaskant.wmsnotes.model

import au.com.console.kassava.kotlinEquals
import java.util.*

abstract class AggregateCommand(val aggId: String) : Command {
    override fun equals(other: Any?) = kotlinEquals(other = other, properties = arrayOf(AggregateCommand::aggId))
    override fun hashCode() = Objects.hash(aggId)
}