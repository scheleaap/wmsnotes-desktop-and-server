package info.maaskant.wmsnotes.testutilities

import assertk.Assert
import assertk.assertions.isInstanceOf
import assertk.assertions.support.expected
import assertk.assertions.support.fail
import assertk.assertions.support.show
import info.maaskant.wmsnotes.client.synchronization.commandexecutor.CommandExecutor
import info.maaskant.wmsnotes.model.CommandError
import kotlin.reflect.KClass

object ExecutionResultAssertions {
    fun Assert<CommandExecutor.ExecutionResult>.asFailure(): Assert<CommandError> = transform {
        when (it) {
            is CommandExecutor.ExecutionResult.Failure -> it.error
            else -> expected("to be:${show(CommandExecutor.ExecutionResult.Failure::class)} but was:${show(it)}")
        }
    }

    fun Assert<CommandExecutor.ExecutionResult>.isFailure() = this.isInstanceOf(CommandExecutor.ExecutionResult.Failure::class)
}