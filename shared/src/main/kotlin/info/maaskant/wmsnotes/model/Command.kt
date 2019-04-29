package info.maaskant.wmsnotes.model

import au.com.console.kassava.kotlinEquals
import java.util.*

abstract class Command(val aggId: String) {
    override fun equals(other: Any?) = kotlinEquals(other = other, properties = arrayOf(Command::aggId))
    override fun hashCode() = Objects.hash(aggId)
}