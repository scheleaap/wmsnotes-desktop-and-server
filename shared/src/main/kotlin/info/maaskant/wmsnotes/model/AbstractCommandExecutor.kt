package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import javax.inject.Inject
import kotlin.reflect.KClass

abstract class AbstractCommandExecutor<
        AggregateType : Aggregate<AggregateType>,
        CommandType : Command,
        RequestType : CommandRequest<CommandType>,
        MapperType : CommandToEventMapper<AggregateType>
        >
@Inject constructor(
        private val commandRequestClass: KClass<RequestType>,
        private val eventStore: EventStore,
        private val repository: AggregateRepository<AggregateType>,
        private val commandToEventMapper: MapperType
) : CommandExecutor<AggregateType, CommandType, RequestType, MapperType> {
    override fun canExecuteRequest(request: CommandRequest<*>): RequestType? {
        @Suppress("NO_REFLECTION_IN_CLASS_PATH")
        return if (commandRequestClass.isInstance(request)) {
            commandRequestClass.javaObjectType.cast(request)
        } else {
            null
        }
    }

    override fun execute(request: RequestType): CommandResult {
        logger.debug("Handling command request: $request")
        val aggregateBefore: AggregateType = repository.getLatest(request.aggId)
        return try {
            val (_, newEvents) = execute(
                    commands = request.commands,
                    aggregateBefore = aggregateBefore,
                    lastRevision = request.lastRevision ?: aggregateBefore.revision,
                    eventsBefore = emptyList())
            CommandResult(
                    requestId = request.requestId,
                    commands = request.commands.map { it to true },
                    newEvents = newEvents
            )
        } catch (t: Throwable) {
            logger.info("Error while process command request $request", t)
            CommandResult(
                    requestId = request.requestId,
                    commands = request.commands.map { it to false },
                    newEvents = emptyList()
            )
        }
    }

    private fun execute(commands: List<CommandType>, aggregateBefore: AggregateType, lastRevision: Int, eventsBefore: List<Event>): Pair<AggregateType, List<Event>> {
        return if (commands.isEmpty()) {
            aggregateBefore to eventsBefore
        } else {
            val (aggregateAfter, newEvents) = execute(commands[0], aggregateBefore, lastRevision)
            execute(
                    commands = commands.drop(1),
                    aggregateBefore = aggregateAfter,
                    lastRevision = aggregateAfter.revision,
                    eventsBefore = eventsBefore + newEvents
            )
        }
    }

    private fun execute(command: CommandType, aggregateBefore: AggregateType, lastRevision: Int): Pair<AggregateType, List<Event>> {
        logger.debug("Executing command: $command")
        val eventIn: Event = commandToEventMapper.map(command, lastRevision = lastRevision)
        val (aggregateAfter, appliedEvent) = aggregateBefore.apply(eventIn)
        return if (appliedEvent != null) {
            logger.debug("Storing event: $appliedEvent")
            val storedEvent = eventStore.appendEvent(appliedEvent)
            aggregateAfter to listOf(storedEvent)
        } else {
            aggregateAfter to emptyList()
        }
    }

    companion object {
        private val logger by logger()

        fun <
                AggregateType : Aggregate<AggregateType>,
                CommandType : Command,
                RequestType : CommandRequest<CommandType>,
                MapperType : CommandToEventMapper<AggregateType>
                >
                connectToBus(commandBus: CommandBus, executor: CommandExecutor<AggregateType, CommandType, RequestType, MapperType>) {
            logger.debug("Connecting command executor $executor to command bus $commandBus")
            commandBus.requests
                    .flatMap {
                        val typedRequest: RequestType? = executor.canExecuteRequest(it)
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
