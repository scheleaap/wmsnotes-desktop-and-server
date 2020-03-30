package info.maaskant.wmsnotes.client.synchronization.commandexecutor

import arrow.core.*
import arrow.core.extensions.list.foldable.firstOption
import info.maaskant.wmsnotes.client.synchronization.CommandToCommandRequestMapper
import info.maaskant.wmsnotes.client.synchronization.commandexecutor.CommandExecutor.EventMetadata
import info.maaskant.wmsnotes.client.synchronization.commandexecutor.CommandExecutor.ExecutionResult
import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.CommandOrigin.REMOTE
import info.maaskant.wmsnotes.utilities.logger
import io.grpc.Status
import org.slf4j.Logger
import javax.inject.Inject
import kotlin.reflect.KFunction2
import kotlin.reflect.KProperty1

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
            is ExecutionResult.Failure -> {
                TODO HIER BEZIG: AFHANKELIJK VAN TYPE FOUT OP DEBUG OF WARNING LOGGEN
                val a: (String) -> Unit = when(executionResult.error) {
                    is CommandError.IllegalStateError -> logger::debug
                    is CommandError.InvalidCommandError -> TODO()
                    is CommandError.NetworkError -> TODO()
                    is CommandError.OtherError -> TODO()
                    is CommandError.StorageError -> TODO()
                }
                logger.debug("Executing command locally failed: $command, $lastRevision, ${executionResult.error}")}
            is ExecutionResult.Success -> logger.debug("Command executed locally successfully: $command")
        }
        return executionResult
    }
}
