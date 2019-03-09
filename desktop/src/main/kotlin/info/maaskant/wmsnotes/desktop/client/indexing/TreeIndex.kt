package info.maaskant.wmsnotes.desktop.client.indexing

import info.maaskant.wmsnotes.desktop.client.indexing.TreeIndex.Change.*
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderEvent
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import info.maaskant.wmsnotes.model.note.NoteDeletedEvent
import info.maaskant.wmsnotes.model.note.NoteUndeletedEvent
import info.maaskant.wmsnotes.utilities.logger
import info.maaskant.wmsnotes.utilities.persistence.StateProducer
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class TreeIndex @Inject constructor(
        eventStore: EventStore,
        initialState: TreeIndexState?,
        scheduler: Scheduler
) : StateProducer<TreeIndexState> {

    private val logger by logger()

    private var state: TreeIndexState = initialState ?: TreeIndexState(/*isInitialized = false*/)
    private val stateUpdates: BehaviorSubject<TreeIndexState> = BehaviorSubject.create()
    private val changes: PublishSubject<Change> = PublishSubject.create()

    init {
        val source = eventStore.getEventUpdates()
//        if (!state.isInitialized) {
//            source = Observable.concat(
//                    eventStore.getEvents()
//                            .doOnSubscribe { logger.debug("Creating initial note index") }
//                            .doOnComplete {
//                                updateState(state.initializationFinished())
//                                logger.debug("Initial note index created")
//                            },
//                    source
//            )
//        }
        source
                .subscribeOn(scheduler)
                .subscribe({
                    when (it) {
                        is NoteCreatedEvent -> noteCreated(it)
                        is NoteDeletedEvent -> noteDeleted(it)
                        is NoteUndeletedEvent -> noteUndeleted(it)
                        is FolderCreatedEvent -> folderCreated(it)
                        else -> TODO()
                    }
                }, { logger.warn("Error", it) })
    }

    private fun addFolderIfNecessary(aggId: String, path: Path) {
        if (path !in state.folders && path != Path()) {
            logger.debug("Adding folder $aggId to index")
            updateState(state.addFolder(path))
            changes.onNext(NodeAdded(Folder(aggId, path, path.elements.last())))
        }
    }

    private fun folderCreated(it: FolderCreatedEvent) =
            addFolderIfNecessary(aggId = it.aggId, path = it.path)

    fun getChanges(): Observable<Change> = changes

    private fun noteCreated(it: NoteCreatedEvent) {
        addFolderIfNecessary(aggId = FolderEvent.aggId(it.path), path = it.path)
        logger.debug("Adding note ${it.aggId} to index")
        val note = Note(it.aggId, it.path, it.title)
        updateState(state.addNote(note))
        changes.onNext(NodeAdded(note))
    }

    private fun noteDeleted(it: NoteDeletedEvent) {
        if (it.aggId in state.notes && it.aggId !in state.hiddenNotes) {
            logger.debug("Hiding note ${it.aggId} in index")
            updateState(state.hideNote(it.aggId))
            changes.onNext(NodeRemoved(it.aggId))

            val note = state.notes.getValue(it.aggId)
            val folderPath = note.path
            val folderAggId = FolderEvent.aggId(folderPath)
            if (folderPath != Path()) {
                updateState(state.removeFolder(folderPath))
                changes.onNext(NodeRemoved(folderAggId))
            }
        }
    }

    private fun noteUndeleted(it: NoteUndeletedEvent) {
        if (it.aggId in state.hiddenNotes) {
            logger.debug("Showing note ${it.aggId} in index")
            updateState(state.unhidNote(it.aggId))
            changes.onNext(NodeAdded(state.notes.getValue(it.aggId)))
        }
    }

    private fun updateState(state: TreeIndexState) {
        this.state = state
        stateUpdates.onNext(state)
    }

    override fun getStateUpdates(): Observable<TreeIndexState> = stateUpdates

    sealed class Change {
        data class NodeAdded(val metadata: Node) : Change()
        data class NodeRemoved(val aggId: String) : Change()
    }
}

