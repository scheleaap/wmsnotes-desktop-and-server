package info.maaskant.wmsnotes.desktop.client.indexing

import info.maaskant.wmsnotes.desktop.client.indexing.TreeIndex.Change.*
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderDeletedEvent
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
                        is NoteCreatedEvent -> handleNoteCreated(it)
                        is NoteDeletedEvent -> handleNoteDeleted(it)
                        is NoteUndeletedEvent -> handleNoteUndeleted(it)
                        is FolderCreatedEvent -> handleFolderCreated(it)
                        is FolderDeletedEvent -> handleFolderDeleted(it)
                        else -> TODO()
                    }
                }, { logger.warn("Error", it) })
    }

    private fun addAutomaticallyGeneratedFoldersIfNecessary(path: Path) {
        val aggId = FolderEvent.aggId(path)
        if (!state.isNodeInFolder(aggId, path) && path.elements.isNotEmpty()) {
            addAutomaticallyGeneratedFoldersIfNecessary(path = path.parent())
            logger.debug("Adding automatically generated folder $path to index")
            val folder = Folder(aggId = aggId, path = path, title = path.elements.last())
            updateState(state.addAutoFolder(folder))
            changes.onNext(NodeAdded(folder))
        }
    }

    private fun addFolder(aggId: String, path: Path) {
        if (path.elements.isNotEmpty()) {
            if (!state.isNodeInFolder(aggId, path)) {
                addAutomaticallyGeneratedFoldersIfNecessary(path = path.parent())
                logger.debug("Adding folder $path to index")
                val folder = Folder(aggId = aggId, path = path, title = path.elements.last())
                updateState(state.addNormalFolder(folder))
                changes.onNext(NodeAdded(folder))
            } else {
                logger.debug("Adding folder $path to index")
                updateState(state.markFolderAsNormal(aggId))
            }
        }
    }

    private fun addNote(note: Note) {
        if (!state.isNodeInFolder(note.aggId, note.path)) {
            addAutomaticallyGeneratedFoldersIfNecessary(path = note.path)
            logger.debug("Adding note ${note.aggId} to index")
            updateState(state.addNote(note))
            changes.onNext(NodeAdded(note))
        }
    }

    fun getChanges(): Observable<Change> = changes

    private fun handleFolderCreated(it: FolderCreatedEvent) =
            addFolder(aggId = it.aggId, path = it.path)

    private fun handleFolderDeleted(it: FolderDeletedEvent) =
            removeFolder(it.path)

    private fun handleNoteCreated(it: NoteCreatedEvent) =
            addNote(Note(it.aggId, it.path, it.title))

    private fun handleNoteDeleted(it: NoteDeletedEvent) =
            removeNote(it.aggId)

    private fun handleNoteUndeleted(it: NoteUndeletedEvent) =
            addNote(state.getNote(it.aggId))

    private fun removeAutomaticallyGeneratedFoldersIfNecessary(path: Path) {
        val aggId = FolderEvent.aggId(path)
        if (state.isAutoFolder(aggId) && path.elements.isNotEmpty()) {
            val children = state.foldersWithChildren.get(path)
            if (children.size == 1 && aggId in children) {
                logger.debug("Removing automatically generated folder $path from index")
                updateState(state.removeAutoFolder(aggId))
                changes.onNext(NodeRemoved(aggId))
                removeAutomaticallyGeneratedFoldersIfNecessary(path.parent())
            }
        }
    }

    private fun removeFolder(path: Path) {
        if (path.elements.isNotEmpty()) {
            logger.debug("Removing folder $path from index")
            val aggId = FolderEvent.aggId(path)
            updateState(state.removeNormalFolder(aggId))
            changes.onNext(NodeRemoved(aggId = aggId))
            removeAutomaticallyGeneratedFoldersIfNecessary(path.parent())
        }
    }

    private fun removeNote(aggId: String) {
        if (aggId in state.notes && state.isNodeInFolder(aggId, state.getNote(aggId).path)) {
            logger.debug("Removing note $aggId from index")
            updateState(state.removeNote(aggId))
            changes.onNext(NodeRemoved(aggId))
            removeAutomaticallyGeneratedFoldersIfNecessary(state.getNote(aggId).path)
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

