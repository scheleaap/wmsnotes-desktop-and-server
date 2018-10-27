package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.api.GrpcCommandMapper
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.CreateNoteCommand
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.server.command.grpc.Command
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import info.maaskant.wmsnotes.utilities.logger
import info.maaskant.wmsnotes.utilities.persistence.StateProducer
import io.grpc.StatusRuntimeException
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class Synchronizer @Inject constructor(
        private val localEvents: ModifiableEventRepository,
        private val remoteEvents: ModifiableEventRepository,
        private val remoteCommandService: CommandServiceGrpc.CommandServiceBlockingStub,
        private val eventToCommandMapper: EventToCommandMapper,
        private val grpcCommandMapper: GrpcCommandMapper,
        private val commandProcessor: CommandProcessor,
        private val noteProjector: NoteProjector,
        initialState: SynchronizerState?
) : StateProducer<SynchronizerState> {

    private val logger by logger()

    private var state = initialState ?: SynchronizerState.create()
    private val stateUpdates: BehaviorSubject<SynchronizerState> = BehaviorSubject.create()
    private val conflictingNoteIdsSubject: Subject<Set<String>> = PublishSubject.create()

    fun synchronize() {
        val localOutboundEvents = localEvents.getEvents().toList().blockingGet()
        val remoteInboundEvents = remoteEvents.getEvents().toList().blockingGet()
        val localOutboundNoteIds = localOutboundEvents.map { it.noteId }
        val remoteInboundNoteIds = remoteInboundEvents.map { it.noteId }
        val conflictingNoteIds = localOutboundNoteIds.intersect(remoteInboundNoteIds)
        updateLastRevisions(localOutboundEvents, remoteInboundEvents)
        processLocalOutboundEvents(localOutboundEvents.filter { it.noteId !in conflictingNoteIds })
        processRemoteInboundEvents(remoteInboundEvents.filter { it.noteId !in conflictingNoteIds })
        conflictingNoteIdsSubject.onNext(conflictingNoteIds)
    }

    private fun updateLastRevisions(localOutboundEvents: List<Event>, remoteInboundEvents: List<Event>) {
        var stateInUpdate = state
        localOutboundEvents.stream().forEach { event ->
            stateInUpdate = stateInUpdate.updateLastLocalRevision(event.noteId, event.revision)
        }
        remoteInboundEvents.stream().forEach { event ->
            stateInUpdate = stateInUpdate.updateLastRemoteRevision(event.noteId, event.revision)
        }
        if (stateInUpdate != state) {
            updateState(stateInUpdate)
        }
    }

    private fun processLocalOutboundEvents(localOutboundEvents: List<Event>) {
        var connectionProblem = false
        val noteIdsWithErrors: MutableSet<String> = HashSet<String>()
        localOutboundEvents
                .stream() // Necessary for filtering to work
                .filter { !connectionProblem && it.noteId !in noteIdsWithErrors }
                .forEach {
                    if (it.eventId !in state.localEventIdsToIgnore) {
                        logger.debug("Processing local event: $it")
                        val command = eventToCommandMapper.map(it, state.lastRemoteRevisions[it.noteId])
                        try {
                            val request = grpcCommandMapper.toGrpcPostCommandRequest(command)
                            logger.debug("Sending command to server: $request")
                            val response = remoteCommandService.postCommand(request)
                            if (response.status == Command.PostCommandResponse.Status.SUCCESS) {
                                localEvents.removeEvent(it)
                                if (response.newEventId > 0) {
                                    updateState(state
                                            .updateLastRemoteRevision(it.noteId, response.newRevision)
                                            .ignoreRemoteEvent(response.newEventId)
                                    )
                                }
                                logger.debug("Local event successfully processed: $it")
                            } else {
                                noteIdsWithErrors += it.noteId
                                logger.debug("Command not processed by server: $request -> ${response.status} ${response.errorDescription}")
                            }
                        } catch (e: StatusRuntimeException) {
                            connectionProblem = true
                            logger.warn("Error sending command to server: ${e.status.code}")
                        }
                    } else {
                        localEvents.removeEvent(it)
                        updateState(state.removeLocalEventToIgnore(it.eventId))
                        logger.debug("Ignored local event $it")
                    }
                }
    }

    private fun processRemoteInboundEvents(remoteInboundEvents: List<Event>) {
        val noteIdsWithErrors: MutableSet<String> = HashSet<String>()
        remoteInboundEvents
                .stream() // Necessary for filtering to work
                .filter { it.noteId !in noteIdsWithErrors }
                .forEach {
                    if (it.eventId !in state.remoteEventIdsToIgnore) {
                        logger.debug("Processing remote event: $it")
                        val command = eventToCommandMapper.map(it, state.lastLocalRevisions[it.noteId])
                        try {
                            processCommandLocallyAndUpdateState(command)
                            remoteEvents.removeEvent(it)
                            logger.debug("Remote event successfully processed: $it")
                        } catch (t: Throwable) {
                            noteIdsWithErrors += it.noteId
                            logger.debug("Command not processed locally: $command + ${state.lastLocalRevisions[it.noteId]}", t)
                        }
                    } else {
                        remoteEvents.removeEvent(it)
                        updateState(state.removeRemoteEventToIgnore(it.eventId))
                        logger.debug("Ignored remote event $it")
                    }
                }
    }

    fun getConflicts(): Observable<Set<String>> {
        return conflictingNoteIdsSubject
    }

    fun resolveConflict(noteId: String, lastLocalRevision: Int, lastRemoteRevision: Int, choice: ConflictResolutionChoice) {
        when (choice) {
            Synchronizer.ConflictResolutionChoice.LOCAL -> {
                remoteEvents
                        .getEvents()
                        .filter { it.noteId == noteId }
                        .filter { it.revision <= lastRemoteRevision }
                        .toList()
                        .blockingGet() // Prevent modifying the underlying collection
                        .forEach { remoteEvents.removeEvent(it) }
            }
            Synchronizer.ConflictResolutionChoice.REMOTE -> {
                localEvents
                        .getEvents()
                        .filter { it.noteId == noteId }
                        .filter { it.revision <= lastLocalRevision }
                        .toList()
                        .blockingGet() // Prevent modifying the underlying collection
                        .forEach { localEvents.removeEvent(it) }
            }
            Synchronizer.ConflictResolutionChoice.BOTH -> {
                val projectedNote = noteProjector.project(noteId, lastLocalRevision)
                val command = CreateNoteCommand(noteId = null, title = projectedNote.title)
                processCommandLocallyAndUpdateState(command)
                localEvents
                        .getEvents()
                        .filter { it.noteId == noteId }
                        .filter { it.revision <= lastLocalRevision }
                        .toList()
                        .blockingGet() // Prevent modifying the underlying collection
                        .forEach { localEvents.removeEvent(it) }

            }
        }
    }

    private fun processCommandLocallyAndUpdateState(command: info.maaskant.wmsnotes.model.Command) {
        val event = commandProcessor.blockingProcessCommand(command)
        if (event != null) {
            updateState(state
                    .updateLastLocalRevision(event.noteId, event.revision)
                    .ignoreLocalEvent(event.eventId)
            )
        }
    }

    private fun updateState(state: SynchronizerState) {
        this.state = state
        stateUpdates.onNext(state)
    }

    override fun getStateUpdates(): Observable<SynchronizerState> = stateUpdates

    enum class ConflictResolutionChoice {
        LOCAL,
        REMOTE,
        BOTH,
    }

}
