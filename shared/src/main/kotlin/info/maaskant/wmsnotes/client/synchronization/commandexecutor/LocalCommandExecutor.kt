package info.maaskant.wmsnotes.client.synchronization.commandexecutor

import arrow.core.*
import info.maaskant.wmsnotes.client.synchronization.CommandToCommandRequestMapper
import info.maaskant.wmsnotes.client.synchronization.commandexecutor.CommandExecutor.EventMetadata
import info.maaskant.wmsnotes.client.synchronization.commandexecutor.CommandExecutor.ExecutionResult
import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.CommandOrigin.REMOTE
import info.maaskant.wmsnotes.utilities.logger
import javax.inject.Inject

@Suppress("MoveVariableDeclarationIntoWhen")
class LocalCommandExecutor @Inject constructor(
        private val commandToCommandRequestMapper: CommandToCommandRequestMapper,
        private val commandBus: CommandBus,
        private val timeout: CommandExecution.Duration
) : CommandExecutor {
    private val logger by logger()

    override fun execute(command: Command, lastRevision: Int): ExecutionResult {
        val executionResult: ExecutionResult = try {
            logger.debug("Executing command locally: $command, $lastRevision")
            val commandRequest = commandToCommandRequestMapper.map(command, lastRevision, origin = REMOTE)
            logger.debug("Mapped command ($command, $lastRevision) to command request $commandRequest")
            val commandResult = CommandExecution.executeBlocking(commandBus, commandRequest, timeout)
            if (commandResult.outcome.size != 1) {
                ExecutionResult.Failure(CommandError.OtherError("Result does not contain 1 command but ${commandResult.outcome.size}: $commandRequest -> $commandResult"))
            } else {
                val outcome: Either<CommandError, Option<Event>> = commandResult.outcome.first().second
                outcome.fold(ExecutionResult::Failure) { eventOption ->
                    ExecutionResult.Success(
                            newEventMetadata = eventOption
                                    .map { event -> EventMetadata(event) }
                                    .getOrElse { null }
                    )
                }
            }
        } catch (t: Throwable) {
            ExecutionResult.Failure(CommandError.OtherError("Unexpected error", cause = Some(t.nonFatalOrThrow())))
        }
        when (executionResult) {
            is ExecutionResult.Failure -> logger.debug("Executing command locally failed: $command, $lastRevision, ${executionResult.error}")
            is ExecutionResult.Success -> logger.debug("Command executed locally successfully: $command")
        }
        return executionResult
    }
}
