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
import java.util.*
import javax.inject.Inject

data class CompensatingAction(
        val compensatedLocalEvents: List<Event>,
        val compensatedRemoteEvents: List<Event>,
        val newLocalEvents: List<Event>,
        val newRemoteEvents: List<Event>
)

class NewSynchronizer @Inject constructor(
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
    private val stateUpdates: BehaviorSubject<SynchronizerState> = BehaviorSubject.create()

    @Synchronized
    fun synchronize() {
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
        synchronizeEvents(eventsToSynchronize)
        removeEventsToIgnore(eventsToIgnore)
    }

    private fun updateLastRevisions(localEvents: Observable<Event>, remoteEvents: Observable<Event>) {
        val state1 = localEvents.scan(state) { state, event -> state.updateLastKnownLocalRevision(event.noteId, event.revision) }
                .last(state)
                .blockingGet()
        val state2 = remoteEvents.scan(state1) { state, event -> state.updateLastKnownRemoteRevision(event.noteId, event.revision) }
                .last(state1)
                .blockingGet()
        if (state2 != state) {
            updateState(state2)
        }
    }

    private fun synchronizeEvents(eventsToSynchronize: SortedMap<String, LocalAndRemoteEvents>) {
        for ((noteId, noteEventsToSynchronize) in eventsToSynchronize) {
            val resolutionResult = synchronizationStrategy.resolve(noteId, noteEventsToSynchronize.localEvents, noteEventsToSynchronize.remoteEvents)
            when (resolutionResult) {
                SynchronizationStrategy.ResolutionResult.NoSolution -> {
                }
                is SynchronizationStrategy.ResolutionResult.Solution ->
                    if (areCompensatingActionsValid(noteEventsToSynchronize.localEvents, noteEventsToSynchronize.remoteEvents, resolutionResult.compensatingActions)) {
                        executeCompensatingActions(resolutionResult.compensatingActions, noteId)
                    } else {
                    }

            }
        }
    }

    private fun removeEventsToIgnore(eventsToIgnore: LocalAndRemoteEvents) {
        eventsToIgnore.remoteEvents.forEach { remoteEvents.removeEvent(it) }
        eventsToIgnore.localEvents.forEach { localEvents.removeEvent(it) }
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

    private fun executeCompensatingActions(compensatingActions: List<CompensatingAction>, noteId: String) {
        if (compensatingActions.isNotEmpty()) {
            val head = compensatingActions[0]
            val success = executeCompensatingAction(head, noteId)
            if (success && compensatingActions.size > 1) {
                val tail = compensatingActions.subList(1, compensatingActions.size)
                executeCompensatingActions(compensatingActions = tail, noteId = noteId)
            }
        }
    }

    private fun executeCompensatingAction(compensatingAction: CompensatingAction, noteId: String): Boolean {
        for (remoteEvent in compensatingAction.newRemoteEvents) {
            val command = eventToCommandMapper.map(remoteEvent, state.lastKnownRemoteRevisions[remoteEvent.noteId])
            val executionResult = remoteCommandExecutor.execute(command)
            when (executionResult) {
                CommandExecutor.ExecutionResult.Failure -> return false
                is CommandExecutor.ExecutionResult.Success -> if (executionResult.newEventMetadata != null) {
                    updateState(state
                            .updateLastKnownRemoteRevision(executionResult.newEventMetadata.noteId, executionResult.newEventMetadata.revision)
                            .ignoreRemoteEvent(executionResult.newEventMetadata.eventId)
                    )
                }
            }
        }
        for (localEvent in compensatingAction.newLocalEvents) {
            val command = eventToCommandMapper.map(localEvent, state.lastKnownLocalRevisions[localEvent.noteId])
            val executionResult = localCommandExecutor.execute(command)
            when (executionResult) {
                CommandExecutor.ExecutionResult.Failure -> return false
                is CommandExecutor.ExecutionResult.Success -> if (executionResult.newEventMetadata != null) {
                    updateState(state
                            .updateLastKnownLocalRevision(executionResult.newEventMetadata.noteId, executionResult.newEventMetadata.revision)
                            .updateLastSynchronizedLocalRevision(executionResult.newEventMetadata.noteId, executionResult.newEventMetadata.revision)
                            .ignoreLocalEvent(executionResult.newEventMetadata.eventId)
                    )
                }
            }
        }
        compensatingAction.compensatedRemoteEvents.forEach { remoteEvents.removeEvent(it) }
        compensatingAction.compensatedLocalEvents.forEach { localEvents.removeEvent(it) }
        if (compensatingAction.compensatedLocalEvents.isNotEmpty()) {
            val lastCompensatedLocalEventRevision = compensatingAction.compensatedLocalEvents.last().revision
            if (state.lastSynchronizedLocalRevisions[noteId] == null || lastCompensatedLocalEventRevision > state.lastSynchronizedLocalRevisions.getValue(noteId)!!) {
                updateState(state
                        .updateLastSynchronizedLocalRevision(noteId, lastCompensatedLocalEventRevision)
                )
            }
        }
        return true
    }

    private fun groupLocalAndRemoteEventsByNote(localEvents: List<Event>, remoteEvents: List<Event>): SortedMap<String, LocalAndRemoteEvents> {
        val localEventsByNote: Map<String, Collection<Event>> = localEvents.toObservable()
                .toMultimap { it.noteId }
                .blockingGet()
        val remoteEventsByNote: Map<String, Collection<Event>> = remoteEvents.toObservable()
                .toMultimap { it.noteId }
                .blockingGet()
        val noteIds = localEventsByNote.keys + remoteEventsByNote.keys
        val result = noteIds.toObservable()
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

    private fun updateState(state: SynchronizerState) {
        this.state = state
        stateUpdates.onNext(state)
    }

    override fun getStateUpdates(): Observable<SynchronizerState> = stateUpdates

    private data class LocalAndRemoteEvents(val localEvents: List<Event>, val remoteEvents: List<Event>)
}
