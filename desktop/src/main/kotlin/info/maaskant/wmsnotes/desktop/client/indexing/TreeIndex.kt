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
import info.maaskant.wmsnotes.model.note.TitleChangedEvent
import info.maaskant.wmsnotes.utilities.logger
import info.maaskant.wmsnotes.utilities.persistence.StateProducer
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.toObservable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class TreeIndex @Inject constructor(
        eventStore: EventStore,
        initialState: TreeIndexState?,
        scheduler: Scheduler
) : StateProducer<TreeIndexState> {

    private val logger by logger()

    private var state: TreeIndexState = initialState ?: TreeIndexState(isInitialized = false)
    private val stateUpdates: BehaviorSubject<TreeIndexState> = BehaviorSubject.create()
    private val changes: PublishSubject<Change> = PublishSubject.create()

    init {
        var source = eventStore.getEventUpdates()
        if (!state.isInitialized) {
            source = Observable.concat(
                    eventStore.getEvents()
                            .doOnSubscribe { logger.debug("Creating initial tree index") }
                            .doOnComplete {
                                updateState(state.initializationFinished())
                                logger.debug("Initial tree index created")
                            },
                    source
            )
        }
        source
                .subscribeOn(scheduler)
                .subscribe({
                    when (it) {
                        is NoteCreatedEvent -> handleNoteCreated(it)
                        is NoteDeletedEvent -> handleNoteDeleted(it)
                        is NoteUndeletedEvent -> handleNoteUndeleted(it)
                        is FolderCreatedEvent -> handleFolderCreated(it)
                        is FolderDeletedEvent -> handleFolderDeleted(it)
                        is TitleChangedEvent -> handleTitleChanged(it)
                        else -> {
                        }
                    }
                }, { logger.warn("Error", it) })
    }

    private fun addAutomaticallyGeneratedFoldersIfNecessary(path: Path): String? {
        if (path.elements.isNotEmpty()) {
            val aggId = FolderEvent.aggId(path)
            if (!state.isNodeInFolder(aggId, path)) {
                val parentAggId = addAutomaticallyGeneratedFoldersIfNecessary(path = path.parent())
                logger.debug("Adding automatically generated folder $path to index")
                val folder = folder(aggId, parentAggId = parentAggId, path = path)
                updateState(state.addAutoFolder(folder))
                changes.onNext(NodeAdded(folder))
            }
            return aggId
        } else {
            return null
        }
    }

    private fun addFolder(aggId: String, path: Path) {
        if (path.elements.isNotEmpty()) {
            if (!state.isNodeInFolder(aggId, path)) {
                val parentAggId = addAutomaticallyGeneratedFoldersIfNecessary(path = path.parent())
                logger.debug("Adding folder $path to index")
                val folder = folder(aggId, parentAggId = parentAggId, path = path)
                updateState(state.addNormalFolder(folder))
                changes.onNext(NodeAdded(folder))
            } else {
                logger.debug("Adding folder $path to index")
                updateState(state.markFolderAsNormal(aggId))
            }
        }
    }

    private fun addNote(aggId: String, path: Path, title: String) {
        if (!state.isNodeInFolder(aggId, path)) {
            val parentAggId = addAutomaticallyGeneratedFoldersIfNecessary(path = path)
            logger.debug("Adding note $aggId to index")
            val note = Note(aggId = aggId, parentAggId = parentAggId, path = path, title = title)
            updateState(state.addNote(note))
            changes.onNext(NodeAdded(note))
        }
    }

    fun getChanges(): Observable<Change> = changes

    fun getExistingNodesAsChanges(): Observable<Change> {
        return state.foldersWithChildren.entries().toObservable()
                .map { (path, aggId) ->
                    if (aggId in state.notes) {
                        NodeAdded(state.notes.getValue(aggId))
                    } else {
                        if (path.elements.size > 1) {
                            NodeAdded(folder(aggId, parentAggId = FolderEvent.aggId(path.parent()), path = path))
                        } else {
                            NodeAdded(folder(aggId, parentAggId = null, path = path))
                        }
                    }
                }
    }

    private fun handleFolderCreated(it: FolderCreatedEvent) =
            addFolder(aggId = it.aggId, path = it.path)

    private fun handleFolderDeleted(it: FolderDeletedEvent) =
            removeFolder(it.path)

    private fun handleNoteCreated(it: NoteCreatedEvent) =
            addNote(aggId = it.aggId, path = it.path, title = it.title)

    private fun handleNoteDeleted(it: NoteDeletedEvent) =
            removeNote(it.aggId)

    private fun handleNoteUndeleted(it: NoteUndeletedEvent) =
            state.notes[it.aggId]?.let { addNote(aggId = it.aggId, path = it.path, title = it.title) }

    private fun handleTitleChanged(it: TitleChangedEvent) {
        val oldNote = state.getNote(aggId = it.aggId)
        val newNote = Note(aggId = oldNote.aggId, parentAggId = oldNote.parentAggId, path = oldNote.path, title = it.title)
        updateState(state.replaceNote(newNote))
        changes.onNext(TitleChanged(it.aggId, it.title))
    }

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
            val aggId = FolderEvent.aggId(path)
            val children = state.foldersWithChildren.get(path)
            if (children.size == 1 && aggId in children) {
                logger.debug("Removing folder $path from index")
                updateState(state.removeNormalFolder(aggId))
                changes.onNext(NodeRemoved(aggId))
                removeAutomaticallyGeneratedFoldersIfNecessary(path.parent())
            } else {
                updateState(state.markFolderAsAuto(aggId))
            }
        }
    }

    private fun removeNote(aggId: String) {
        if (aggId in state.notes) {
            val path = state.getNote(aggId).path
            if (state.isNodeInFolder(aggId, path)) {
                logger.debug("Removing note $aggId from index")
                updateState(state.removeNote(aggId))
                changes.onNext(NodeRemoved(aggId))
                removeAutomaticallyGeneratedFoldersIfNecessary(path)
            }
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
        data class TitleChanged(val aggId: String, val title: String) : Change()
    }

    companion object {
        private fun folder(aggId: String, parentAggId: String?, path: Path) =
                Folder(aggId = aggId, parentAggId = parentAggId, path = path, title = path.elements.last())
    }
}

