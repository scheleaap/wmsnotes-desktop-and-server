package info.maaskant.wmsnotes.desktop.main

import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.folder.Folder
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.springframework.stereotype.Component
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Component
class NavigationViewModel @Inject constructor(
        private val folderRepository: AggregateRepository<Folder>,
        private val noteRepository: AggregateRepository<Note>
) {

    private val logger by logger()

    final val selectionRequest: Subject<SelectionRequest> = PublishSubject.create<SelectionRequest>().toSerialized()
    final val currentSelection: Subject<Selection> = BehaviorSubject.createDefault<Selection>(Selection.Nothing).toSerialized()
    final var currentPathValue: Path = Path()
    final var currentNoteValue: Note? = null
    final val selectionSwitchingProcess: Subject<SelectionSwitchingProcessNotification> = PublishSubject.create<SelectionSwitchingProcessNotification>().toSerialized()
    final val isLoading: Subject<Boolean> = PublishSubject.create<Boolean>().toSerialized()
    private val isNavigationAllowed: Subject<Boolean> = PublishSubject.create<Boolean>().toSerialized()

    init {
        Observables.combineLatest(selectionRequest, isNavigationAllowed)
                .observeOn(Schedulers.io())
                .filter { (_, isNavigationAllowed) -> isNavigationAllowed }
                .map { (selectionRequest, _) -> selectionRequest }
                .distinctUntilChanged()
                .switchMap(::loadRequestedSelection)
                .subscribe(selectionSwitchingProcess)
        selectionSwitchingProcess
                .subscribe(::handleSelectionSwitchingProcessNotification)
        isNavigationAllowed.subscribe { logger.debug("Navigation allowed? $it") }
        selectionRequest.subscribe { logger.debug("Selection request: $it") }
        currentSelection.subscribe { logger.debug("Selection changed to: $it") }
    }

    private fun loadRequestedSelection(selectionRequest: SelectionRequest): Observable<SelectionSwitchingProcessNotification> {
        // The following article has some useful information that we might be able to use to improve this code:
        // https://tech.pic-collage.com/rxandroid-handle-interrupt-with-switchmap-3a650393299f
        return when (selectionRequest) {
            SelectionRequest.NothingSelectionRequest -> {
                Observable.just(SelectionSwitchingProcessNotification.Nothing as SelectionSwitchingProcessNotification)
            }
            is SelectionRequest.NoteSelectionRequest -> {
                noteRepository.getAndUpdate(selectionRequest.aggId)
                        .subscribeOn(Schedulers.io())
                        .map { SelectionSwitchingProcessNotification.Note(it) as SelectionSwitchingProcessNotification }
                        .startWith(SelectionSwitchingProcessNotification.Loading(true))
            }
            is SelectionRequest.FolderSelectionRequest -> {
                Observable.just(SelectionSwitchingProcessNotification.Folder(selectionRequest = selectionRequest) as SelectionSwitchingProcessNotification)
            }
        }.concatMap {
            if (it !is SelectionSwitchingProcessNotification.Loading) {
                Observable.just(it, SelectionSwitchingProcessNotification.Loading(false))
            } else {
                Observable.just(it)
            }
        }
    }

    private fun handleSelectionSwitchingProcessNotification(it: SelectionSwitchingProcessNotification) {
        @Suppress("UNUSED_VARIABLE")
        val a = when (it) { // Assign to variable to force a compilation error if 'when' expression is not exhaustive.
            is SelectionSwitchingProcessNotification.Loading -> isLoading.onNext(it.loading)
            SelectionSwitchingProcessNotification.Nothing -> {
                currentSelection.onNext(Selection.Nothing)
                currentPathValue = Path()
                currentNoteValue = null
            }
            is SelectionSwitchingProcessNotification.Note -> {
                if (it.note.exists) {
                    currentSelection.onNext(Selection.NoteSelection(
                            aggId = it.note.aggId,
                            path = it.note.path,
                            title = it.note.title
                    ))
                    currentPathValue = it.note.path
                    currentNoteValue = it.note
                } else {
                    currentSelection.onNext(Selection.Nothing)
                    currentNoteValue = null
                }
            }
            is SelectionSwitchingProcessNotification.Folder -> {
                currentSelection.onNext(Selection.FolderSelection(
                        aggId = it.selectionRequest.aggId,
                        path = it.selectionRequest.path,
                        title = it.selectionRequest.title
                ))
                currentPathValue = it.selectionRequest.path
                currentNoteValue = null
            }
        }
    }

    fun start() {
        selectionRequest.onNext(SelectionRequest.NothingSelectionRequest)
    }

    fun setNavigationAllowed(observable: Observable<Boolean>) {
        observable.subscribe(this.isNavigationAllowed)
    }

    sealed class SelectionRequest {
        object NothingSelectionRequest : SelectionRequest()
        data class NoteSelectionRequest(val aggId: String) : SelectionRequest()
        data class FolderSelectionRequest(val aggId: String, val path: Path, val title: String) : SelectionRequest()
    }

    sealed class Selection {
        object Nothing : Selection()
        data class NoteSelection(val aggId: String, val path: Path, val title: String) : Selection()
        data class FolderSelection(val aggId: String, val path: Path, val title: String) : Selection()
    }

    sealed class SelectionSwitchingProcessNotification {
        data class Loading(val loading: Boolean) : SelectionSwitchingProcessNotification()
        object Nothing : SelectionSwitchingProcessNotification()
        data class Note(val note: info.maaskant.wmsnotes.model.note.Note) : SelectionSwitchingProcessNotification()
        data class Folder(val selectionRequest: SelectionRequest.FolderSelectionRequest) : SelectionSwitchingProcessNotification()
    }
}