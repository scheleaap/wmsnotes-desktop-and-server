package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.note.NoteCommandToEventMapper
import info.maaskant.wmsnotes.utilities.logger
import javax.inject.Inject

abstract class AggregateCommandExecutor<
        AggregateType : Aggregate<AggregateType>,
        CommandType : AggregateCommand,
        CommandRequestType : AggregateCommandRequest<CommandType>,
        MapperType : CommandToEventMapper<AggregateType>
        >
@Inject constructor(
        private val eventStore: EventStore,
        private val repository: AggregateRepository<AggregateType>,
        private val commandToEventMapper: MapperType
) {
    private val logger by logger()

    fun execute(request: CommandRequestType): CommandResult {
        val aggregate: AggregateType = repository.getLatest(request.aggId)
        return try {
            execute(request.commands, aggregate, request.lastRevision ?: aggregate.revision)
            CommandResult.Success(request.requestId)
        } catch (t: Throwable) {
            logger.info("Failed to execute command request $request", t)
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
        val eventIn: Event = commandToEventMapper.map(command, lastRevision = lastRevision)
        val (aggregateAfter, eventOut) = aggregateBefore.apply(eventIn)
        if (eventOut != null) {
            eventStore.appendEvent(eventOut)
        }
        return aggregateAfter
    }

//    companion object {
//        fun connectToBus(commandBus: CommandBus, executor: AggregateCommandExecutor<*>) {
//            commandBus.requests
//                    .filter { executor.canHandle(it) }
//                    .map {
//                        val o2 = executor.asCommandRequestClass(it)
//                        val result: HandlingResult = executor.handle(o2)
//                        when (result) {
//                            NotHandled -> TODO()
//                            is Handled -> TODO()
//                        }
//                    }
//        }
//    }
}
