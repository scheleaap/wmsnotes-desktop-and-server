package info.maaskant.wmsnotes.model

import arrow.core.*
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.extensions.list.foldable.firstOption
import arrow.syntax.collections.tail
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.utilities.ApplicationService
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
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
                        onError = { logger.error("Error", it) }
                )
    }

    override fun execute(request: RequestType): CommandResult {
        logger.debug("Executing command request: {}", request)
        val aggregateBefore: AggregateType = repository.getLatest(request.aggId)
        return CommandResult(
                requestId = request.requestId,
                outcome = execute(
                        commands = request.commands,
                        aggregate = aggregateBefore,
                        lastRevision = request.lastRevision ?: aggregateBefore.revision
                ),
                origin = request.origin
        )
    }

    private fun execute(
            commands: List<CommandType>,
            aggregate: AggregateType,
            lastRevision: Int
    ): ImmutableList<Pair<Command, Either<CommandError, Option<Event>>>> {
        val head: Option<CommandType> = commands.firstOption()
        val tail: List<CommandType> = commands.tail()
        return head.map { command ->
            val result: Either<CommandError, Pair<AggregateType, Option<Event>>> = execute(command, aggregate, lastRevision)
            val eventEither = result.map { it.second }
            when (result) {
                is Left -> persistentListOf(command to eventEither)
                is Right -> {
                    val aggregateAfter = result.b.first
                    persistentListOf(command to eventEither) + execute(
                            aggregate = aggregateAfter,
                            lastRevision = aggregateAfter.revision,
                            commands = tail
                    )
                }
            }
        }.getOrElse { persistentListOf() }
    }

    private fun execute(
            command: CommandType,
            aggregateBefore: AggregateType,
            lastRevision: Int
    ): Either<CommandError, Pair<AggregateType, Option<Event>>> {
        return try {
            logger.debug("Executing command: {}", command)
            val eventIn: Event = commandToEventMapper.map(command, lastRevision = lastRevision)
            val (aggregateAfter, appliedEvent) = aggregateBefore.apply(eventIn)
            val r = if (appliedEvent != null) {
                logger.debug("Command {} produced event: {}, storing", command, appliedEvent)
                eventStore.appendEvent(appliedEvent)
                        .map { aggregateAfter to Some(it) }
            } else {
                logger.debug("Command {} produced no event", command)
                Right(aggregateAfter to None)
            }
            logger.debug("Command executed successfully: $command")
            r
        } catch (t: Throwable) {
            val message = "Executing command failed ($command, $aggregateBefore, $lastRevision, $t)"
            logger.warn(message)
            Left(CommandError.OtherError(message, cause = Some(t.nonFatalOrThrow())))
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
