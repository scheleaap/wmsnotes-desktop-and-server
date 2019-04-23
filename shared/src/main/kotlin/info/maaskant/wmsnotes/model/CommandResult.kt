package info.maaskant.wmsnotes.model

import au.com.console.kassava.SupportsMixedTypeEquality
import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinToString
import info.maaskant.wmsnotes.model.note.CreateNoteCommand
import java.util.*

sealed class CommandResult(val requestId: Int) : SupportsMixedTypeEquality {
    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(CommandResult::requestId)
    )

    override fun hashCode() = Objects.hashCode(requestId)

    class Failure(requestId: Int) : CommandResult(requestId) {
        override fun canEqual(other: Any?) = other is Failure

        override fun toString() = kotlinToString(properties = arrayOf(Failure::requestId))
    }

    class Success(requestId: Int) : CommandResult(requestId) {
        override fun canEqual(other: Any?) = other is Success

        override fun toString() = kotlinToString(properties = arrayOf(Success::requestId))
    }
}
