package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.synchronization.commandexecutor.CommandExecutor
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy
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
        val success = synchronizeEvents(eventsToSynchronize)
        return SynchronizationResult(success = success)
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

    private fun synchronizeEvents(eventsToSynchronize: SortedMap<String, LocalAndRemoteEvents>) : Boolean {
        val r = eventsToSynchronize.asSequence().map { (aggId, aggregateEventsToSynchronize) ->
            aggId to (aggregateEventsToSynchronize to resolve(aggregateEventsToSynchronize, aggId))
        }.filter { (aggId, tmp) ->
            val (aggregateEventsToSynchronize, resolutionResult) = tmp
            when (resolutionResult) {
                SynchronizationStrategy.ResolutionResult.NoSolution -> {
                    logger.warn("The events for aggregate {} cannot be synchronized, because the program does not know how. Events: {}", aggId, aggregateEventsToSynchronize)
                    false
                }
                is SynchronizationStrategy.ResolutionResult.Solution ->
                    if (areCompensatingActionsValid(aggregateEventsToSynchronize.localEvents, aggregateEventsToSynchronize.remoteEvents, resolutionResult.compensatingActions)) {
                        true
                    } else {
                        logger.warn("The compensating actions for aggregate {} are invalid: {}, {}, {}", aggId, aggregateEventsToSynchronize.localEvents, aggregateEventsToSynchronize.remoteEvents, resolutionResult.compensatingActions)
                        false
                    }
            }
        }.map { (aggId, tmp) ->
            val (_, resolutionResult) = tmp
            aggId to (resolutionResult as SynchronizationStrategy.ResolutionResult.Solution).compensatingActions
        }.map { (aggId, compensatingActions) ->
            executeCompensatingActions(compensatingActions, aggId)
        }.toList()

        return r.all { it }
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

    private fun areCompensatingActionsValid(
            localEventsToSynchronize: List<Event>,
            remoteEventsToSynchronize: List<Event>,
            compensatingActions: List<CompensatingAction>
    ): Boolean {
        val localEventIdsToSynchronize: List<Int> = localEventsToSynchronize.map { it.eventId }.sorted()
        val remoteEventIdsToSynchronize: List<Int> = remoteEventsToSynchronize.map { it.eventId }.sorted()
        val compensatedLocalEventIds: List<Int> = compensatingActions.toObservable()
                .flatMap { it.compensatedLocalEvents.toObservable() }
                .map { it.eventId }
                .sorted()
                .toList()
                .blockingGet()
        val compensatedRemoteEventIds: List<Int> = compensatingActions.toObservable()
                .flatMap { it.compensatedRemoteEvents.toObservable() }
                .map { it.eventId }
                .sorted()
                .toList()
                .blockingGet()
        return localEventIdsToSynchronize == compensatedLocalEventIds && remoteEventIdsToSynchronize == compensatedRemoteEventIds
    }

    private fun executeCompensatingActions(compensatingActions: List<CompensatingAction>, aggId: String): Boolean {
        return if (compensatingActions.isNotEmpty()) {
            val head = compensatingActions[0]
            val tail = compensatingActions.subList(1, compensatingActions.size)
            logger.debug("Executing compensating action for aggregate {}: {}", aggId, head)
            val success = executeCompensatingAction(head, aggId)
            if (success) {
                logger.debug("Successfully executed compensating action for aggregate {}: {}", aggId, head)
                executeCompensatingActions(compensatingActions = tail, aggId = aggId)
            } else {
                logger.warn("Failed to execute compensating action for aggregate {}, skipping further compensating actions: {}", aggId, head)
                false
            }
        } else {
            true
        }
    }

    private fun executeCompensatingAction(compensatingAction: CompensatingAction, aggId: String): Boolean {
        for (remoteEvent in compensatingAction.newRemoteEvents) {
            val command = eventToCommandMapper.map(remoteEvent)
            val lastRevision = state.lastKnownRemoteRevisions[remoteEvent.aggId] ?: 0
            val executionResult = remoteCommandExecutor.execute(command, lastRevision)
            logger.debug("Remote event {} -> command {} + lastRevision {} -> {}", remoteEvent, command, lastRevision, executionResult)
            when (executionResult) {
                CommandExecutor.ExecutionResult.Failure -> return false
                is CommandExecutor.ExecutionResult.Success -> if (executionResult.newEventMetadata != null) {
                    updateState(state
                            .updateLastKnownRemoteRevision(executionResult.newEventMetadata.aggId, executionResult.newEventMetadata.revision)
                            .ignoreRemoteEvent(executionResult.newEventMetadata.eventId)
                    )
                }
            }
        }
        for (localEvent in compensatingAction.newLocalEvents) {
            val command = eventToCommandMapper.map(localEvent)
            val lastRevision = state.lastKnownLocalRevisions[localEvent.aggId] ?: 0
            val executionResult = localCommandExecutor.execute(command, lastRevision)
            logger.debug("Local event {} -> command {} + lastRevision {} -> {}", localEvent, command, lastRevision, executionResult)
            when (executionResult) {
                CommandExecutor.ExecutionResult.Failure -> return false
                is CommandExecutor.ExecutionResult.Success -> if (executionResult.newEventMetadata != null) {
                    updateState(state
                            .updateLastKnownLocalRevision(executionResult.newEventMetadata.aggId, executionResult.newEventMetadata.revision)
                            .updateLastSynchronizedLocalRevision(executionResult.newEventMetadata.aggId, executionResult.newEventMetadata.revision)
                            .ignoreLocalEvent(executionResult.newEventMetadata.eventId)
                    )
                }
            }
        }
        compensatingAction.compensatedRemoteEvents.forEach { remoteEvents.removeEvent(it) }
        compensatingAction.compensatedLocalEvents.forEach { localEvents.removeEvent(it) }
        if (compensatingAction.compensatedLocalEvents.isNotEmpty()) {
            val lastCompensatedLocalEventRevision = compensatingAction.compensatedLocalEvents.last().revision
            if (state.lastSynchronizedLocalRevisions[aggId] == null || lastCompensatedLocalEventRevision > state.lastSynchronizedLocalRevisions.getValue(aggId)!!) {
                updateState(state
                        .updateLastSynchronizedLocalRevision(aggId, lastCompensatedLocalEventRevision)
                )
            }
        }
        return true
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
