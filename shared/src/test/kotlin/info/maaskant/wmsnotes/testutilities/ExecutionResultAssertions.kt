package info.maaskant.wmsnotes.testutilities

import assertk.Assert
import assertk.assertions.support.expected
import info.maaskant.wmsnotes.client.synchronization.commandexecutor.CommandExecutor

object ExecutionResultAssertions {
    fun Assert<CommandExecutor.ExecutionResult>.isFailure() = given {
        val failureMessage = "expected ExecutionResult to be Failure, but was $it"
        when (it) {
            is CommandExecutor.ExecutionResult.Failure -> {
            }
            is CommandExecutor.ExecutionResult.Success -> expected(failureMessage)
        }
    }
}