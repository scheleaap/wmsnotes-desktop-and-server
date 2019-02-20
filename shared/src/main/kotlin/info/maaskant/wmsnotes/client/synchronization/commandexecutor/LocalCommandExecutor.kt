package info.maaskant.wmsnotes.client.synchronization.commandexecutor

import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.utilities.logger
import javax.inject.Inject

class LocalCommandExecutor @Inject constructor(private val commandProcessor: CommandProcessor) : CommandExecutor {
    private val logger by logger()

    override fun execute(command: Command): CommandExecutor.ExecutionResult =
            try {
                command
                        .let {
                            logger.debug("Executing command locally: $command")
                            commandProcessor.blockingProcessCommand(command)
                        }
                        ?.let { CommandExecutor.EventMetadata(event = it) }
                        .let { CommandExecutor.ExecutionResult.Success(newEventMetadata = it) }
                        .let {
                            logger.debug("Command successfully executed locally: $command")
                            it
                        }
            } catch (t: Throwable) {
                logger.debug("Executing command $command locally failed", t)
                CommandExecutor.ExecutionResult.Failure
            }
}
