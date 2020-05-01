package info.maaskant.wmsnotes.client.synchronization

import arrow.core.Either
import arrow.core.Either.Companion.left
import arrow.core.Either.Companion.right
import info.maaskant.wmsnotes.client.synchronization.commandexecutor.CommandExecutor
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult.Solution
import info.maaskant.wmsnotes.model.CommandError
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.utilities.logger
import info.maaskant.wmsnotes.utilities.persistence.StateProducer
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.*
import javax.inject.Inject

class Synchronizer @Inject constructor(
        private val localEvents: ModifiableEventRepository,
        private val remoteEvents: ModifiableEventRepository,
        private val synchronizationStrategy: SynchronizationStrategy,
        private val eventToCommandMapper: EventToCommandMapper,
        private val localCommandExecutor: CommandExecutor,
        private val remoteCommandExecutor: CommandExecutor,
        initialState: SynchronizerState?
) : StateProducer<SynchronizerState> {

    private val logger by logger()

    private var state = initialState ?: SynchronizerState.create()
    private val stateUpdates: Subject<SynchronizerState> = BehaviorSubject.create<SynchronizerState>().toSerialized()

    @Synchronized
    fun synchronize(): SynchronizationResult {
        val localEvents = localEvents.getEvents().publish().refCount()
        val remoteEvents = remoteEvents.getEvents().publish().refCount()
        val localEventsToSynchronize: List<Event> = localEvents
                .filter { it.eventId !in state.localEventIdsToIgnore }
                .toList()
                .blockingGet()
        val remoteEventsToSynchronize: List<Event> = remoteEvents
                .filter { it.eventId !in state.remoteEventIdsToIgnore }
                .toList()
                .blockingGet()
        val localEventsToIgnore: List<Event> = localEvents
                .filter { it.eventId in state.localEventIdsToIgnore }
                .toList()
                .blockingGet()
        val remoteEventsToIgnore: List<Event> = remoteEvents
                .filter { it.eventId in state.remoteEventIdsToIgnore }
                .toList()
                .blockingGet()

        val eventsToSynchronize: SortedMap<String, LocalAndRemoteEvents> = groupLocalAndRemoteEventsByNote(localEventsToSynchronize, remoteEventsToSynchronize)
        val eventsToIgnore = LocalAndRemoteEvents(localEventsToIgnore, remoteEventsToIgnore)

        updateLastRevisions(localEvents, remoteEvents)
        removeEventsToIgnore(eventsToIgnore)
        val errors = synchronizeEvents(eventsToSynchronize)
        return SynchronizationResult(errors = errors)
    }

    private fun updateLastRevisions(localEvents: Observable<Event>, remoteEvents: Observable<Event>) {
        val state1 = localEvents.scan(state) { state, event ->
            val oldRevision = state.lastKnownLocalRevisions[event.aggId] ?: 0
            val newRevision = event.revision
            if (oldRevision < newRevision) {
                logger.debug("Updating last known local revision of aggregate {} from {} to {}", event.aggId, oldRevision, newRevision)
                state.updateLastKnownLocalRevision(event.aggId, newRevision)
            } else {
                state
            }
        }
                .last(state)
                .blockingGet()
        val state2 = remoteEvents.scan(state1) { state, event ->
            val oldRevision = state.lastKnownRemoteRevisions[event.aggId] ?: 0
            val newRevision = event.revision
            if (oldRevision < newRevision) {
                logger.debug("Updating last known remote revision of aggregate {} from {} to {}", event.aggId, oldRevision, newRevision)
                state.updateLastKnownRemoteRevision(event.aggId, newRevision)
            } else {
                state
            }
        }
                .last(state1)
                .blockingGet()
        if (state2 != state) {
            updateState(state2)
        }
    }

    private fun synchronizeEvents(eventsToSynchronize: SortedMap<String, LocalAndRemoteEvents>): List<Pair<String, CommandError>> {
        val r = eventsToSynchronize.asSequence().map { (aggId, aggregateEventsToSynchronize) ->
            aggId to (aggregateEventsToSynchronize to resolve(aggregateEventsToSynchronize, aggId))
        }.filter { (aggId, tmp) ->
            val (aggregateEventsToSynchronize: LocalAndRemoteEvents, resolutionResult: SynchronizationStrategy.ResolutionResult) = tmp
            when (resolutionResult) {
                NoSolution -> {
                    logger.warn("The events for aggregate {} cannot be synchronized, because the program does not know how. Events: {}", aggId, aggregateEventsToSynchronize)
                    false
                }
                is Solution ->
                    if (isSolutionValid(aggregateEventsToSynchronize.localEvents, aggregateEventsToSynchronize.remoteEvents, resolutionResult)) {
                        true
                    } else {
                        logger.warn("The solution for aggregate {} are invalid: {}, {}, {}", aggId, aggregateEventsToSynchronize.localEvents, aggregateEventsToSynchronize.remoteEvents, resolutionResult)
                        false
                    }
            }
        }.map { (aggId, tmp) ->
            val (_, resolutionResult) = tmp
            aggId to (resolutionResult as Solution)
        }.map { (aggId, compensatingActions) ->
            aggId to executeSolution(compensatingActions, aggId)
        }.toList() // Create a list first to ensure that all aggregates are processed.

        return r.flatMap { (aggId, result) ->
            result.fold({ listOf(aggId to it) }, { emptyList() })
        }
    }

    private fun removeEventsToIgnore(eventsToIgnore: LocalAndRemoteEvents) {
        eventsToIgnore.remoteEvents.forEach {
            logger.debug("Ignoring remote event ${it.eventId}")
            remoteEvents.removeEvent(it)
            updateState(state.removeRemoteEventToIgnore(it.eventId))
        }
        eventsToIgnore.localEvents.forEach {
            logger.debug("Ignoring local event ${it.eventId}")
            localEvents.removeEvent(it)
            updateState(state.removeLocalEventToIgnore(it.eventId))
        }
    }

    private fun isSolutionValid(
            localEventsToSynchronize: List<Event>,
            remoteEventsToSynchronize: List<Event>,
            solution: Solution
    ): Boolean {
        val localEventIdsToSynchronize: List<Int> = localEventsToSynchronize.map { it.eventId }.sorted()
        val remoteEventIdsToSynchronize: List<Int> = remoteEventsToSynchronize.map { it.eventId }.sorted()
        val compensatedLocalEventIds: List<Int> = solution.compensatedLocalEvents.asSequence()
                .map { it.eventId }
                .sorted()
                .toList()
        val compensatedRemoteEventIds: List<Int> = solution.compensatedRemoteEvents.asSequence()
                .map { it.eventId }
                .sorted()
                .toList()
        return localEventIdsToSynchronize == compensatedLocalEventIds && remoteEventIdsToSynchronize == compensatedRemoteEventIds
    }

    private fun executeSolution(solution: Solution, aggId: String): Either<CommandError, Unit> {
        logger.debug("Executing solution for aggregate {}: {}", aggId, solution)
        return executeSolution2(solution, aggId).map {
            logger.debug("Successfully executed solution for aggregate {}: {}", aggId, solution)
        }.mapLeft {
            logger.warn("Failed to execute solution for aggregate {}: {}", aggId, solution)
            it
        }
    }

    private fun executeSolution2(solution: Solution, aggId: String): Either<CommandError, Unit> {
        if (solution.newRemoteEvents.isNotEmpty() || solution.newLocalEvents.isNotEmpty()) {
            for (remoteEvent in solution.newRemoteEvents) {
                val command = eventToCommandMapper.map(remoteEvent)
                val lastRevision = state.lastKnownRemoteRevisions[remoteEvent.aggId] ?: 0
                val executionResult = remoteCommandExecutor.execute(command, lastRevision)
                logger.debug("Remote event {} -> command {} + lastRevision {} -> {}", remoteEvent, command, lastRevision, executionResult)
                when (executionResult) {
                    is CommandExecutor.ExecutionResult.Failure -> return left(executionResult.error)
                    is CommandExecutor.ExecutionResult.Success -> if (executionResult.newEventMetadata != null) {
                        updateState(state
                                .updateLastKnownRemoteRevision(executionResult.newEventMetadata.aggId, executionResult.newEventMetadata.revision)
                                .ignoreRemoteEvent(executionResult.newEventMetadata.eventId)
                        )
                    }
                }
            }
            for (localEvent in solution.newLocalEvents) {
                val command = eventToCommandMapper.map(localEvent)
                val lastRevision = state.lastKnownLocalRevisions[localEvent.aggId] ?: 0
                val executionResult = localCommandExecutor.execute(command, lastRevision)
                logger.debug("Local event {} -> command {} + lastRevision {} -> {}", localEvent, command, lastRevision, executionResult)
                when (executionResult) {
                    is CommandExecutor.ExecutionResult.Failure -> return left(executionResult.error)
                    is CommandExecutor.ExecutionResult.Success -> if (executionResult.newEventMetadata != null) {
                        updateState(state
                                .updateLastKnownLocalRevision(executionResult.newEventMetadata.aggId, executionResult.newEventMetadata.revision)
                                .updateLastSynchronizedLocalRevision(executionResult.newEventMetadata.aggId, executionResult.newEventMetadata.revision)
                                .ignoreLocalEvent(executionResult.newEventMetadata.eventId)
                        )
                    }
                }
            }
        }
        solution.compensatedRemoteEvents.forEach { remoteEvents.removeEvent(it) }
        solution.compensatedLocalEvents.forEach { localEvents.removeEvent(it) }
        if (solution.compensatedLocalEvents.isNotEmpty()) {
            val lastCompensatedLocalEventRevision = solution.compensatedLocalEvents.last().revision
            if (state.lastSynchronizedLocalRevisions[aggId] == null || lastCompensatedLocalEventRevision > state.lastSynchronizedLocalRevisions.getValue(aggId)!!) {
                updateState(state
                        .updateLastSynchronizedLocalRevision(aggId, lastCompensatedLocalEventRevision)
                )
            }
        }
        return right(Unit)
    }

    private fun groupLocalAndRemoteEventsByNote(localEvents: List<Event>, remoteEvents: List<Event>): SortedMap<String, LocalAndRemoteEvents> {
        val localEventsByNote: Map<String, Collection<Event>> = localEvents.toObservable()
                .toMultimap { it.aggId }
                .blockingGet()
        val remoteEventsByNote: Map<String, Collection<Event>> = remoteEvents.toObservable()
                .toMultimap { it.aggId }
                .blockingGet()
        val aggId = localEventsByNote.keys + remoteEventsByNote.keys
        @Suppress("UnnecessaryVariable") val result = aggId.toObservable()
                .toMap({ it }, {
                    LocalAndRemoteEvents(
                            localEvents = localEventsByNote[it]?.toList() ?: emptyList(),
                            remoteEvents = remoteEventsByNote[it]?.toList() ?: emptyList()
                    )
                })
                .blockingGet()
                .toSortedMap()
        return result
    }

    private fun resolve(aggregateEventsToSynchronize: LocalAndRemoteEvents, aggId: String): SynchronizationStrategy.ResolutionResult {
        logger.debug("Synchronizing events for aggregate {}: {}", aggId, aggregateEventsToSynchronize)
        val resolutionResult = synchronizationStrategy.resolve(aggId, aggregateEventsToSynchronize.localEvents, aggregateEventsToSynchronize.remoteEvents)
        logger.debug("Resolution result for aggregate {}: {}", aggId, resolutionResult)
        return resolutionResult
    }

    private fun updateState(state: SynchronizerState) {
        this.state = state
        stateUpdates.onNext(state)
    }

    override fun getStateUpdates(): Observable<SynchronizerState> = stateUpdates
}
