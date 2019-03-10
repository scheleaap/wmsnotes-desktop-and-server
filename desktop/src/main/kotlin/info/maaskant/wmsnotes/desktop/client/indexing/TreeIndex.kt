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
                        is NoteCreatedEvent -> noteCreated(it)
                        is NoteDeletedEvent -> noteDeleted(it)
                        is NoteUndeletedEvent -> noteUndeleted(it)
                        is FolderCreatedEvent -> folderCreated(it)
                        is FolderDeletedEvent -> folderDeleted(it)
                        else -> TODO()
                    }
                }, { logger.warn("Error", it) })
    }

    private fun addAutomaticallyGeneratedFoldersIfNecessary(path: Path) {
        val aggId = FolderEvent.aggId(path)
        if (aggId !in state.foldersWithChildren.get(path) && path.elements.isNotEmpty()) {
            addAutomaticallyGeneratedFoldersIfNecessary(path = path.parent())
            logger.debug("Adding automatically generated folder $path to index")
            val folder = Folder(aggId = aggId, path = path, title = path.elements.last())
            changes.onNext(NodeAdded(folder))
        }
    }

    private fun addFolder(aggId: String, path: Path) {
        if (aggId !in state.foldersWithChildren.get(path) && path.elements.isNotEmpty()) {
            addAutomaticallyGeneratedFoldersIfNecessary(path = path.parent())
            logger.debug("Adding folder $path to index")
            val folder = Folder(aggId = aggId, path = path, title = path.elements.last())
            updateState(state.addFolder(folder))
            changes.onNext(NodeAdded(folder))
        }
    }

    private fun addNote(note: Note) {
        addAutomaticallyGeneratedFoldersIfNecessary(path = note.path)
        logger.debug("Adding note ${note.aggId} to index")
        updateState(state.addNote(note))
        changes.onNext(NodeAdded(note))
    }

    private fun folderCreated(it: FolderCreatedEvent) =
            addFolder(aggId = it.aggId, path = it.path)

    private fun folderDeleted(it: FolderDeletedEvent) =
            removeFolderIfNecessary(it.path)

    fun getChanges(): Observable<Change> = changes

    private fun noteCreated(it: NoteCreatedEvent) =
            addNote(Note(it.aggId, it.path, it.title))

    private fun noteDeleted(it: NoteDeletedEvent) {
        if (it.aggId in state.notes && it.aggId !in state.hiddenNotes) {
            logger.debug("Removing note ${it.aggId} from index")
            updateState(state.removeNote(it.aggId))
            changes.onNext(NodeRemoved(it.aggId))
            removeAutomaticallyGeneratedFoldersIfNecessary(state.notes.getValue(it.aggId).path)
        }
    }

    private fun removeAutomaticallyGeneratedFoldersIfNecessary(path: Path) {
        if (path.elements.isNotEmpty() && state.foldersWithChildren.get(path).isEmpty()) {
            logger.debug("Removing automatically generated folder $path from index")
            val aggId = FolderEvent.aggId(path)
            changes.onNext(NodeRemoved(aggId))
            removeAutomaticallyGeneratedFoldersIfNecessary(path.parent())
        }
    }

    private fun removeFolderIfNecessary(path: Path) {
        if (path.elements.isNotEmpty()) {
            logger.debug("Removing folder $path from index")
            val aggId = FolderEvent.aggId(path)
            updateState(state.removeFolder(aggId))
            removeAutomaticallyGeneratedFoldersIfNecessary(path)
        }
    }

    private fun noteUndeleted(it: NoteUndeletedEvent) =
            addNote(state.notes.getValue(it.aggId))

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

