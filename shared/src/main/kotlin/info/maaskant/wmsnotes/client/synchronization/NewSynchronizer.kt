package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
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
        private val compensatingActionExecutor: CompensatingActionExecutor,
        initialState: SynchronizerState?
) : StateProducer<SynchronizerState> {

    private val logger by logger()

    private var state = initialState ?: SynchronizerState.create()
    private val stateUpdates: BehaviorSubject<SynchronizerState> = BehaviorSubject.create()

    @Synchronized
    fun synchronize() {
        val localEventsToSynchronize: List<Event> = localEvents.getEvents().toList().blockingGet()
        val remoteEventsToSynchronize: List<Event> = remoteEvents.getEvents().toList().blockingGet()

        val r: SortedMap<String, LocalAndRemoteEvents> = groupLocalAndRemoteEventsByNote(localEventsToSynchronize, remoteEventsToSynchronize)

        for ((noteId, eventsToSynchronize) in r) {
            val resolutionResult = synchronizationStrategy.resolve(eventsToSynchronize.localEvents, eventsToSynchronize.remoteEvents)
            @Suppress("UNUSED_VARIABLE")
            val a: Unit = when (resolutionResult) { // Assign to variable to force a compilation error if 'when' expression is not exhaustive.
                SynchronizationStrategy.ResolutionResult.NoSolution -> {
                }
                is SynchronizationStrategy.ResolutionResult.Solution ->
                    if (areCompensatingActionsValid(eventsToSynchronize.localEvents, eventsToSynchronize.remoteEvents, resolutionResult.compensatingActions)) {
                        executeCompensatingActions(resolutionResult.compensatingActions, noteId)
                    } else {
                    }

            }
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
        val executionResult = compensatingActionExecutor.execute(compensatingAction)
        if (executionResult.success) {
            updateState(state
                    .run {
                        executionResult.newLocalEvents.lastOrNull()?.let {
                            this
                                    .updateLastKnownLocalRevision(noteId, it.revision)
                                    .updateLastSynchronizedLocalRevision(noteId, it.revision)
                        } ?: this
                    }
                    .run {
                        executionResult.newRemoteEvents.lastOrNull()?.let {
                            this.updateLastKnownRemoteRevision(noteId, it.revision)
                        } ?: this
                    }
            )
            compensatingAction.compensatedRemoteEvents.forEach { remoteEvents.removeEvent(it) }
            compensatingAction.compensatedLocalEvents.forEach { localEvents.removeEvent(it) }
        }
        return executionResult.success
    }

    private fun updateState(state: SynchronizerState) {
        this.state = state
        stateUpdates.onNext(state)
    }

    override fun getStateUpdates(): Observable<SynchronizerState> = stateUpdates

    private data class LocalAndRemoteEvents(val localEvents: List<Event>, val remoteEvents: List<Event>)
}
