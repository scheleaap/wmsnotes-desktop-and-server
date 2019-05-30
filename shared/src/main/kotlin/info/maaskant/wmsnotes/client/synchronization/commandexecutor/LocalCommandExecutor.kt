package info.maaskant.wmsnotes.client.synchronization.commandexecutor

import info.maaskant.wmsnotes.client.synchronization.CommandToCommandRequestMapper
import info.maaskant.wmsnotes.client.synchronization.commandexecutor.CommandExecutor.EventMetadata
import info.maaskant.wmsnotes.client.synchronization.commandexecutor.CommandExecutor.ExecutionResult
import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.CommandBus
import info.maaskant.wmsnotes.model.CommandExecution
import info.maaskant.wmsnotes.model.CommandOrigin.REMOTE
import info.maaskant.wmsnotes.utilities.logger
import javax.inject.Inject

class LocalCommandExecutor @Inject constructor(
        private val commandToCommandRequestMapper: CommandToCommandRequestMapper,
        private val commandBus: CommandBus,
        private val timeout: CommandExecution.Duration
) : CommandExecutor {
    private val logger by logger()

    override fun execute(command: Command, lastRevision: Int): ExecutionResult {
        val (executionResult: ExecutionResult, throwable: Throwable?) = try {
            command
                    .let {
                        logger.debug("Executing command locally: ($command, $lastRevision)")
                        val commandRequest = commandToCommandRequestMapper.map(command, lastRevision, origin = REMOTE)
                        logger.debug("Mapped command ($command, $lastRevision) to command request $commandRequest")
                        CommandExecution.executeBlocking(commandBus, commandRequest, timeout)
                    }
                    .let {
                        if (it.allSuccessful) {
                            it.newEvents.firstOrNull()
                                    ?.let { EventMetadata(event = it) }
                                    .let { ExecutionResult.Success(newEventMetadata = it) }
                        } else {
                            ExecutionResult.Failure
                        } to null
                    }
        } catch (t: Throwable) {
            ExecutionResult.Failure to t
        }
        when (executionResult) {
            ExecutionResult.Failure -> logger.debug("Executing command ($command, $lastRevision) locally failed", throwable)
            is ExecutionResult.Success -> logger.debug("Command successfully executed locally: ($command, $lastRevision)")
        }
        return executionResult
    }
}
