package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.utilities.ApplicationService
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import java.lang.RuntimeException
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
        private val commandBus: CommandBus,
        private val eventStore: EventStore,
        private val repository: AggregateRepository<AggregateType>,
        private val commandToEventMapper: MapperType,
        private val scheduler: Scheduler
) : CommandExecutor<AggregateType, CommandType, RequestType, MapperType>, ApplicationService {

    private val logger by logger()

    private var disposable: Disposable? = null

    override fun canExecuteRequest(request: CommandRequest<*>): RequestType? {
        @Suppress("NO_REFLECTION_IN_CLASS_PATH")
        return if (commandRequestClass.isInstance(request)) {
            commandRequestClass.javaObjectType.cast(request)
        } else {
            null
        }
    }

    private fun connect(): Disposable {
        return commandBus.requests
                .observeOn(scheduler)
                .flatMap {
                    val typedRequest: RequestType? = canExecuteRequest(it)
                    if (typedRequest != null) {
                        Observable.just(typedRequest)
                    } else {
                        Observable.empty()
                    }
                }
                .map { execute(it) }
                .subscribeBy(
                        onNext = commandBus.results::onNext,
                        onError = { logger.warn("Error", it) }
                )
    }

    override fun execute(request: RequestType): CommandResult {
        logger.debug("Executing command request: {}", request)
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
                    newEvents = newEvents,
                    origin = request.origin
            )
        } catch (t: Throwable) {
            logger.info("Error while executing command request $request", t)
            CommandResult(
                    requestId = request.requestId,
                    commands = request.commands.map { it to false },
                    newEvents = emptyList(),
                    origin = request.origin
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
        logger.debug("Executing command: {}", command)
        val eventIn: Event = commandToEventMapper.map(command, lastRevision = lastRevision)
        val (aggregateAfter, appliedEvent) = aggregateBefore.apply(eventIn)
        return if (appliedEvent != null) {
            logger.debug("Command {} produced event: {}, storing", command, appliedEvent)
            val storedEvent = eventStore.appendEvent(appliedEvent)
            aggregateAfter to listOf(storedEvent)
        } else {
            logger.debug("Command {} produced no event", command)
            aggregateAfter to emptyList()
        }
    }

    @Synchronized
    override fun start() {
        if (disposable == null) {
            logger.debug("Starting")
            disposable = connect()
        }
    }

    @Synchronized
    override fun shutdown() {
        disposable?.let {
            logger.debug("Shutting down")
            it.dispose()
        }
        disposable = null
    }
}
