package info.maaskant.wmsnotes.desktop.client.indexing

import info.maaskant.wmsnotes.desktop.client.indexing.TreeIndex.Change.*
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.folder.Folder.Companion.aggId
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderDeletedEvent
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

private typealias New = Pair<TreeIndexState, List<TreeIndex.Change>>

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
                    val (newState, newChanges) = when (it) {
                        is NoteCreatedEvent -> handleNoteCreated(state, it)
                        is NoteDeletedEvent -> handleNoteDeleted(state, it)
                        is NoteUndeletedEvent -> handleNoteUndeleted(state, it)
                        is FolderCreatedEvent -> handleFolderCreated(state, it)
                        is FolderDeletedEvent -> handleFolderDeleted(state, it)
                        is TitleChangedEvent -> handleTitleChanged(state, it)
                        else -> state to emptyList()
                    }
                    if (state != newState) {
                        updateState(newState)
                    }
                    newChanges.forEach { changes.onNext(it) }

                }, { logger.warn("Error", it) })
    }

    private fun addAutomaticallyGeneratedFoldersIfNecessary(state: TreeIndexState, changes: List<Change>, path: Path): Triple<String?, TreeIndexState, List<Change>> {
        return if (path.elements.isNotEmpty()) {
            val aggId = aggId(path)
            if (!state.isNodeInFolder(aggId, path.parent())) {
                val (parentAggId, newState, newChanges) = addAutomaticallyGeneratedFoldersIfNecessary(state, changes, path.parent())
                logger.debug("Adding automatically generated folder $path to index")
                val folder = folder(aggId, parentAggId = parentAggId, path = path)
                Triple(aggId, newState.addAutoFolder(folder), newChanges + NodeAdded(folder, folderIndex = 0))
            } else {
                Triple(aggId, state, changes)
            }
        } else {
            Triple(null, state, changes)
        }
    }

    private fun addFolder(state: TreeIndexState, aggId: String, path: Path): New {
        return if (path.elements.isNotEmpty()) {
            if (!state.isNodeInFolder(aggId, path.parent())) {
                val (parentAggId, newState1, newChanges) = addAutomaticallyGeneratedFoldersIfNecessary(state, emptyList(), path = path.parent())
                logger.debug("Adding folder $path to index")
                val folder = folder(aggId, parentAggId = parentAggId, path = path)
                val newState2 = newState1.addNormalFolder(folder)
                val folderIndex = calculateFolderIndex(newState2, path.parent(), aggId)
                newState2 to newChanges + NodeAdded(folder, folderIndex = folderIndex)
            } else {
                logger.debug("Adding folder $path to index")
                state.markFolderAsNormal(aggId) to emptyList()
            }
        } else {
            state to emptyList()
        }
    }

    private fun addNote(state: TreeIndexState, aggId: String, path: Path, title: String): New {
        return if (!state.isNodeInFolder(aggId, path)) {
            val (parentAggId, newState1, newChanges) = addAutomaticallyGeneratedFoldersIfNecessary(state, emptyList(), path = path)
            logger.debug("Adding note $aggId to index")
            val note = Note(aggId = aggId, parentAggId = parentAggId, path = path, title = title)
            val newState2 = newState1.addNote(note)
            val folderIndex = calculateFolderIndex(newState2, path, aggId)
            newState2 to newChanges + NodeAdded(note, folderIndex = folderIndex)
        } else {
            state to emptyList()
        }
    }

    fun getChanges(): Observable<Change> = changes

    fun getExistingNodesAsChanges(): Observable<Change> {
        return state.foldersWithChildren.entries().toObservable()
                .map { (_, aggId) ->
                    val (node: Node, folderIndex: Int) = when (aggId) {
                        in state.notes -> {
                            val note = state.notes.getValue(aggId)
                            val folderIndex = calculateFolderIndex(state, note.path, aggId)
                            note to folderIndex
                        }
                        in state.folders -> {
                            val folder = state.folders.getValue(aggId)
                            val folderIndex = calculateFolderIndex(state, folder.path.parent(), aggId)
                            folder to folderIndex
                        }
                        else -> throw IllegalStateException("Unknown aggregate '$aggId'")
                    }
                    NodeAdded(node, folderIndex = folderIndex)
                }
    }

    private fun calculateFolderIndex(state: TreeIndexState, path: Path, aggId: String) =
            state.foldersWithChildren[path].indexOf(aggId)

    private fun handleFolderCreated(state: TreeIndexState, it: FolderCreatedEvent) =
            addFolder(state, aggId = it.aggId, path = it.path)

    private fun handleFolderDeleted(state: TreeIndexState, it: FolderDeletedEvent) =
            removeFolder(state, it.path)

    private fun handleNoteCreated(state: TreeIndexState, it: NoteCreatedEvent) =
            addNote(state, aggId = it.aggId, path = it.path, title = it.title)

    private fun handleNoteDeleted(state: TreeIndexState, it: NoteDeletedEvent) =
            removeNote(state, it.aggId)

    private fun handleNoteUndeleted(state: TreeIndexState, it: NoteUndeletedEvent): New =
            state.notes[it.aggId]?.let { addNote(state, aggId = it.aggId, path = it.path, title = it.title) }
                    ?: state to emptyList()

    private fun handleTitleChanged(state: TreeIndexState, it: TitleChangedEvent): New {
        val oldNote = state.getNote(aggId = it.aggId)
        val newNote = Note(aggId = oldNote.aggId, parentAggId = oldNote.parentAggId, path = oldNote.path, title = it.title)
        return state.replaceNote(newNote) to listOf(TitleChanged(it.aggId, it.title))
    }

    private fun removeAutomaticallyGeneratedFoldersIfNecessary(state: TreeIndexState, changes: List<Change>, path: Path): New {
        val aggId = aggId(path)
        return if (state.isAutoFolder(aggId) && path.elements.isNotEmpty()) {
            val children = state.foldersWithChildren.get(path)
            if (children.isEmpty()) {
                logger.debug("Removing automatically generated folder $path from index")
                removeAutomaticallyGeneratedFoldersIfNecessary(
                        state.removeAutoFolder(aggId),
                        changes + NodeRemoved(aggId),
                        path.parent()
                )
            } else {
                state to changes
            }
        } else {
            state to changes
        }
    }

    private fun removeFolder(state: TreeIndexState, path: Path): New {
        return if (path.elements.isNotEmpty()) {
            val aggId = aggId(path)
            val children = state.foldersWithChildren.get(path)
            if (children.isEmpty()) {
                logger.debug("Removing folder $path from index")
                removeAutomaticallyGeneratedFoldersIfNecessary(
                        state.removeNormalFolder(aggId),
                        listOf(NodeRemoved(aggId)),
                        path.parent()
                )
            } else {
                state.markFolderAsAuto(aggId) to emptyList()
            }
        } else {
            state to emptyList()
        }
    }

    private fun removeNote(state: TreeIndexState, aggId: String): New {
        return if (aggId in state.notes) {
            val path = state.getNote(aggId).path
            if (state.isNodeInFolder(aggId, path)) {
                logger.debug("Removing note $aggId from index")
                removeAutomaticallyGeneratedFoldersIfNecessary(
                        state.removeNote(aggId),
                        listOf(NodeRemoved(aggId)),
                        path
                )
            } else {
                state to emptyList()
            }
        } else {
            state to emptyList()
        }
    }

    private fun updateState(state: TreeIndexState) {
        this.state = state
        stateUpdates.onNext(state)
    }

    override fun getStateUpdates(): Observable<TreeIndexState> = stateUpdates

    sealed class Change {
        data class NodeAdded(val metadata: Node, val folderIndex: Int) : Change()
        data class NodeRemoved(val aggId: String) : Change()
        data class TitleChanged(val aggId: String, val title: String) : Change()
    }

    companion object {
        private fun folder(aggId: String, parentAggId: String?, path: Path) =
                Folder(aggId = aggId, parentAggId = parentAggId, path = path, title = path.elements.last())
    }
}

