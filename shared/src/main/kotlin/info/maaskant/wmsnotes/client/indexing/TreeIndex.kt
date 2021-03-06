package info.maaskant.wmsnotes.client.indexing

import info.maaskant.wmsnotes.client.indexing.TreeIndexEvent.*
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.folder.Folder.Companion.aggId
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderDeletedEvent
import info.maaskant.wmsnotes.model.note.*
import info.maaskant.wmsnotes.utilities.ApplicationService
import info.maaskant.wmsnotes.utilities.logger
import info.maaskant.wmsnotes.utilities.persistence.StateProducer
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toObservable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

private typealias New = Pair<TreeIndexState, List<TreeIndexEvent>>

@Singleton
class TreeIndex @Inject constructor(
        private val eventStore: EventStore,
        private val sortingStrategy: Comparator<Node>,
        initialState: TreeIndexState?,
        private val scheduler: Scheduler
) : StateProducer<TreeIndexState>, ApplicationService {

    private val logger by logger()

    private var disposable: Disposable? = null

    private val events: Subject<TreeIndexEvent> = PublishSubject.create<TreeIndexEvent>().toSerialized()

    private var state: TreeIndexState = initialState ?: TreeIndexState(isInitialized = false)

    private val stateUpdates: Subject<TreeIndexState> = BehaviorSubject.create<TreeIndexState>().toSerialized()

    private fun connect(): Disposable {
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
        return source
                .subscribeOn(scheduler)
                .subscribeBy(onNext = {
                    val (newState, newEvents) = when (it) {
                        is NoteCreatedEvent -> handleNoteCreated(state, it)
                        is NoteDeletedEvent -> handleNoteDeleted(state, it)
                        is NoteUndeletedEvent -> handleNoteUndeleted(state, it)
                        is FolderCreatedEvent -> handleFolderCreated(state, it)
                        is FolderDeletedEvent -> handleFolderDeleted(state, it)
                        is MovedEvent -> handleMoved(state,it)
                        is TitleChangedEvent -> handleTitleChanged(state, it)
                        else -> state to emptyList()
                    }
                    if (state != newState) {
                        updateState(newState)
                    }
                    newEvents.forEach(events::onNext)

                }, onError = { logger.error("Error", it) })
    }

    private fun addAutomaticallyGeneratedFoldersIfNecessary(state: TreeIndexState, events: List<TreeIndexEvent>, path: Path): Triple<String?, TreeIndexState, List<TreeIndexEvent>> {
        return if (!path.isRoot) {
            val aggId = aggId(path)
            if (!state.isNodeInFolder(aggId, path.parent())) {
                val (parentAggId, newState, newEvents) = addAutomaticallyGeneratedFoldersIfNecessary(state, events, path.parent())
                logger.debug("Adding automatically generated folder $path to index")
                val folder = folder(aggId, parentAggId = parentAggId, path = path)
                Triple(aggId, newState.addAutoFolder(folder), newEvents + NodeAdded(folder, folderIndex = 0))
            } else {
                Triple(aggId, state, events)
            }
        } else {
            Triple(null, state, events)
        }
    }

    private fun addFolder(state: TreeIndexState, aggId: String, path: Path): New {
        return if (!path.isRoot) {
            if (!state.isNodeInFolder(aggId, path.parent())) {
                val (parentAggId, newState1, newEvents) = addAutomaticallyGeneratedFoldersIfNecessary(state, emptyList(), path = path.parent())
                logger.debug("Adding folder $path to index")
                val folder = folder(aggId, parentAggId = parentAggId, path = path)
                val newState2 = newState1.addNormalFolder(folder)
                val folderIndex = calculateFolderIndex(newState2, path.parent(), aggId)
                newState2 to newEvents + NodeAdded(folder, folderIndex = folderIndex)
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
            val (parentAggId, newState1, newEvents) = addAutomaticallyGeneratedFoldersIfNecessary(state, emptyList(), path = path)
            logger.debug("Adding note $aggId to index")
            val note = Note(aggId = aggId, parentAggId = parentAggId, path = path, title = title)
            val newState2 = newState1.addNote(note)
            val folderIndex = calculateFolderIndex(newState2, path, aggId)
            newState2 to newEvents + NodeAdded(note, folderIndex = folderIndex)
        } else {
            state to emptyList()
        }
    }

    private fun calculateFolderIndex(state: TreeIndexState, path: Path, aggId: String): Int {
        return state.foldersWithChildren[path]
                .map { aggId2 ->
                    when (aggId2) {
                        in state.notes -> state.getNote(aggId2)
                        in state.folders -> state.getFolder(aggId2)
                        else -> throw IllegalStateException("Unknown aggregate '$aggId2'")
                    }
                }
                .sortedWith(sortingStrategy)
                .map { it.aggId }
                .indexOf(aggId)
    }

    fun getEvents(filterByFolder: Path? = null): Observable<TreeIndexEvent> =
            events
                    .filter {
                        filterByFolder == null || when (it) {
                            is NodeAdded -> getParentPath(it.node) == filterByFolder
                            is NodeRemoved -> getParentPath(it.node) == filterByFolder
                            is TitleChanged -> getParentPath(it.node) == filterByFolder
                        }
                    }

    fun getNodes(filterByFolder: Path? = null): Observable<IndexedValue<Node>> {
        val keySequence = if (filterByFolder == null) {
            state.foldersWithChildren.asMap().keys.asSequence()
        } else {
            sequenceOf(filterByFolder)
        }
        return keySequence
                .flatMap { path ->
                    state.foldersWithChildren[path]
                            .map { aggId ->
                                when (aggId) {
                                    in state.notes -> state.getNote(aggId)
                                    in state.folders -> state.getFolder(aggId)
                                    else -> throw IllegalStateException("Unknown aggregate '$aggId'")
                                }
                            }
                            .sortedWith(sortingStrategy)
                            .withIndex()
                            .asSequence()
                }
                .toObservable()
    }

    private fun getParentPath(node: Node): Path {
        return when (node) {
            is Folder -> node.path.parent()
            is Note -> node.path
        }
    }

    private fun handleFolderCreated(state: TreeIndexState, it: FolderCreatedEvent) =
            addFolder(state, aggId = it.aggId, path = it.path)

    private fun handleFolderDeleted(state: TreeIndexState, it: FolderDeletedEvent) =
            removeFolder(state, it.path)

    private fun handleMoved(state: TreeIndexState, it: MovedEvent): New {
        val (newState1, newEvents1) = removeNote(state, it.aggId)
        val oldNote: Note = state.getNote(aggId = it.aggId)
        val (newState2, newEvents2) = addNote(newState1, it.aggId, it.path, oldNote.title)
        return newState2 to newEvents1 + newEvents2
    }

    private fun handleNoteCreated(state: TreeIndexState, it: NoteCreatedEvent) =
            addNote(state, aggId = it.aggId, path = it.path, title = it.title)

    private fun handleNoteDeleted(state: TreeIndexState, it: NoteDeletedEvent) =
            removeNote(state, it.aggId)

    private fun handleNoteUndeleted(state: TreeIndexState, it: NoteUndeletedEvent): New =
            state.notes[it.aggId]?.let { addNote(state, aggId = it.aggId, path = it.path, title = it.title) }
                    ?: state to emptyList()

    private fun handleTitleChanged(state: TreeIndexState, it: TitleChangedEvent): New {
        val oldNote: Note = state.getNote(aggId = it.aggId)
        val newNote: Note = Note(aggId = oldNote.aggId, parentAggId = oldNote.parentAggId, path = oldNote.path, title = it.title)
        val newState = state.replaceNote(newNote)
        val oldFolderIndex = calculateFolderIndex(state, oldNote.path, it.aggId)
        val newFolderIndex = calculateFolderIndex(newState, oldNote.path, it.aggId)
        return newState to listOf(TitleChanged(newNote, oldFolderIndex = oldFolderIndex, newFolderIndex = newFolderIndex))
    }

    private fun removeAutomaticallyGeneratedFoldersIfNecessary(state: TreeIndexState, events: List<TreeIndexEvent>, path: Path): New {
        val aggId = aggId(path)
        return if (state.isAutoFolder(aggId) && !path.isRoot) {
            val children = state.foldersWithChildren.get(path)
            if (children.isEmpty()) {
                logger.debug("Removing automatically generated folder $path from index")
                val folder = state.getFolder(aggId)
                removeAutomaticallyGeneratedFoldersIfNecessary(
                        state.removeAutoFolder(aggId),
                        events + NodeRemoved(folder),
                        path.parent()
                )
            } else {
                state to events
            }
        } else {
            state to events
        }
    }

    private fun removeFolder(state: TreeIndexState, path: Path): New {
        return if (!path.isRoot) {
            val aggId = aggId(path)
            val children = state.foldersWithChildren.get(path)
            if (children.isEmpty()) {
                logger.debug("Removing folder $path from index")
                val folder = state.getFolder(aggId)
                removeAutomaticallyGeneratedFoldersIfNecessary(
                        state.removeNormalFolder(aggId),
                        listOf(NodeRemoved(folder)),
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
            val note = state.getNote(aggId)
            if (state.isNodeInFolder(aggId, note.path)) {
                logger.debug("Removing note $aggId from index")
                removeAutomaticallyGeneratedFoldersIfNecessary(
                        state.removeNote(aggId),
                        listOf(NodeRemoved(note)),
                        note.path
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

    companion object {
        fun asNodeAddedEvents(): ObservableTransformer<IndexedValue<Node>, NodeAdded> {
            return ObservableTransformer { it.map { it2 -> NodeAdded(it2.value, folderIndex = it2.index) } }
        }

        private fun folder(aggId: String, parentAggId: String?, path: Path) =
                Folder(aggId = aggId, parentAggId = parentAggId, path = path, title = path.elements.last())
    }
}

