package info.maaskant.wmsnotes.desktop.main

import info.maaskant.wmsnotes.client.indexing.NoteIndex
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.utilities.Optional
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import io.reactivex.observables.ConnectableObservable
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
        eventStore: EventStore,
        noteIndex: NoteIndex,
        private val noteProjector: NoteProjector
) {

    private val logger by logger()

    final val allEventsWithUpdates: ConnectableObservable<Event> = noteIndex.getNotes()
            .subscribeOn(Schedulers.io())
            .map { NoteCreatedEvent(0, it.noteId, 0, it.title) as Event }
            .mergeWith(eventStore.getEventUpdates())
            .publish()

    // TODO: Replace with SerializedSubject
    final val selectionRequest: Subject<Selection> = PublishSubject.create()
    final val currentSelection: BehaviorSubject<Selection> = BehaviorSubject.createDefault(Selection.Nothing)
    final var currentNoteValue: Note? = null
    private final val currentNote: BehaviorSubject<Optional<Note>> = BehaviorSubject.createDefault(Optional<Note>())
    final val selectionSwitchingProcess: Subject<SelectionSwitchingProcessNotification> = PublishSubject.create()
    final val isLoading: Subject<Boolean> = PublishSubject.create()
    private val isNavigationAllowed: Subject<Boolean> = PublishSubject.create()

    init {
        Observables.combineLatest(selectionRequest, isNavigationAllowed)
                .observeOn(Schedulers.io())
                .filter { (selectionRequest, isNavigationAllowed) -> isNavigationAllowed && selectionRequest != currentSelection.value }
                .map { (selectionRequest, _) -> selectionRequest }
                .distinctUntilChanged()
                .switchMap(::loadRequestedSelection)
                .subscribe(selectionSwitchingProcess)
        selectionSwitchingProcess
                .subscribe {
                    when (it) {
                        is NavigationViewModel.SelectionSwitchingProcessNotification.Loading -> isLoading.onNext(it.loading)
                        NavigationViewModel.SelectionSwitchingProcessNotification.Nothing -> {
                            currentSelection.onNext(Selection.Nothing)
                            currentNote.onNext(Optional())
                            currentNoteValue = null
                        }
                        is NavigationViewModel.SelectionSwitchingProcessNotification.Note -> {
                            currentSelection.onNext(it.selection)
                            currentNote.onNext(Optional(it.note))
                            currentNoteValue = it.note
                        }
                    }
                }
        isNavigationAllowed.subscribe { logger.debug("Navigation allowed? $it") }
        selectionRequest.subscribe { logger.debug("Selection request: $it") }
        currentSelection.subscribe { logger.debug("Selection changed to: $it") }
        currentNote.subscribe { logger.debug("Note changed to: $it") }
    }

    private fun loadRequestedSelection(selectionRequest: Selection): Observable<SelectionSwitchingProcessNotification> =
    // The following article has some useful information that we might be able to use to improve this code:
    // https://tech.pic-collage.com/rxandroid-handle-interrupt-with-switchmap-3a650393299f
            when (selectionRequest) {
                Selection.Nothing -> {
                    Observable.just(SelectionSwitchingProcessNotification.Nothing as SelectionSwitchingProcessNotification)
                }
                is Selection.NoteSelection -> {
                    noteProjector.projectAndUpdate(selectionRequest.noteId)
                            .subscribeOn(Schedulers.io())
                            .map { SelectionSwitchingProcessNotification.Note(selectionRequest, it) as SelectionSwitchingProcessNotification }
                            .startWith(SelectionSwitchingProcessNotification.Loading(true))
                }
                is Selection.FolderSelection -> {
                    TODO()
                }
            }.concatMap { it ->
                if (it !is SelectionSwitchingProcessNotification.Loading) {
                    Observable.just(it, SelectionSwitchingProcessNotification.Loading(false))
                } else {
                    Observable.just(it)
                }
            }

    fun start() {
        allEventsWithUpdates.connect()
        selectionRequest.onNext(Selection.Nothing)
    }

    fun setNavigationAllowed(observable: Observable<Boolean>) {
        observable.subscribe(this.isNavigationAllowed)
    }

    sealed class Selection {
        object Nothing : Selection()
        data class NoteSelection(val noteId: String, val title: String) : Selection()
        data class FolderSelection(val path: String, val title: String) : Selection()
    }

    sealed class SelectionSwitchingProcessNotification {
        data class Loading(val loading: Boolean) : SelectionSwitchingProcessNotification()
        object Nothing : SelectionSwitchingProcessNotification()
        data class Note(val selection: Selection.NoteSelection, val note: info.maaskant.wmsnotes.model.projection.Note) : SelectionSwitchingProcessNotification()
    }
}