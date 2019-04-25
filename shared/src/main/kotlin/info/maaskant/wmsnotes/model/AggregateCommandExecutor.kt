package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import javax.inject.Inject
import kotlin.reflect.KClass

abstract class AggregateCommandExecutor<
        AggregateType : Aggregate<AggregateType>,
        CommandType : AggregateCommand,
        CommandRequestType : AggregateCommandRequest<CommandType>,
        MapperType : CommandToEventMapper<AggregateType>
        >
@Inject constructor(
        private val commandRequestClass: KClass<CommandRequestType>,
        private val eventStore: EventStore,
        private val repository: AggregateRepository<AggregateType>,
        private val commandToEventMapper: MapperType
) {
    fun canExecuteRequest(request: CommandRequest): CommandRequestType? {
        @Suppress("NO_REFLECTION_IN_CLASS_PATH")
        return if (commandRequestClass.isInstance(request)) {
            commandRequestClass.javaObjectType.cast(request)
        } else {
            null
        }
    }

    fun execute(request: CommandRequestType): CommandResult {
        logger.debug("Handling command request: $request")
        val aggregate: AggregateType = repository.getLatest(request.aggId)
        return try {
            execute(request.commands, aggregate, request.lastRevision ?: aggregate.revision)
            CommandResult.Success(request.requestId)
        } catch (t: Throwable) {
            logger.info("Error while process command request $request", t)
            CommandResult.Failure(request.requestId)
        }
    }

    private fun execute(commands: List<CommandType>, aggregateBefore: AggregateType, lastRevision: Int): AggregateType {
        return if (commands.isEmpty()) {
            aggregateBefore
        } else {
            val aggregateAfter = execute(commands[0], aggregateBefore, lastRevision)
            execute(commands.drop(1), aggregateAfter, aggregateAfter.revision)
        }
    }

    private fun execute(command: CommandType, aggregateBefore: AggregateType, lastRevision: Int): AggregateType {
        logger.debug("Executing command: $command")
        val eventIn: Event = commandToEventMapper.map(command, lastRevision = lastRevision)
        val (aggregateAfter, eventOut) = aggregateBefore.apply(eventIn)
        if (eventOut != null) {
            logger.debug("Storing event: $eventOut")
            eventStore.appendEvent(eventOut)
        }
        return aggregateAfter
    }

    companion object {
        private val logger by logger()

        fun <
                AggregateType : Aggregate<AggregateType>,
                CommandType : AggregateCommand,
                CommandRequestType : AggregateCommandRequest<CommandType>,
                MapperType : CommandToEventMapper<AggregateType>
                >
                connectToBus(commandBus: CommandBus, executor: AggregateCommandExecutor<AggregateType, CommandType, CommandRequestType, MapperType>) {
            logger.debug("Connecting command executor $executor to command bus $commandBus")
            commandBus.requests
                    .flatMap {
                        val typedRequest: CommandRequestType? = executor.canExecuteRequest(it)
                        if (typedRequest != null) {
                            Observable.just(typedRequest)
                        } else {
                            Observable.empty()
                        }
                    }
                    .map { executor.execute(it) }
                    .doOnSubscribe {
                        logger.debug("Connected command executor $executor to command bus $commandBus")
                    }
                    .subscribe(commandBus.results)
        }
    }
}
