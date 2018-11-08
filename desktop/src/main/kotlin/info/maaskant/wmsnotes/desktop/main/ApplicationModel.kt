package info.maaskant.wmsnotes.desktop.main

import info.maaskant.wmsnotes.client.indexing.NoteIndex
import info.maaskant.wmsnotes.desktop.main.editing.EditingViewModel
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.utilities.Optional
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import io.reactivex.observables.ConnectableObservable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.springframework.stereotype.Component
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Component
class ApplicationModel @Inject constructor(
        eventStore: EventStore,
        noteIndex: NoteIndex,
        private val noteProjector: NoteProjector,
        private val editingViewModel: EditingViewModel
) {

    private val logger by logger()

    val allEventsWithUpdates: ConnectableObservable<Event> = noteIndex.getNotes()
            .subscribeOn(Schedulers.io())
            .map { NoteCreatedEvent(0, it.noteId, 0, it.title) as Event }
            .mergeWith(eventStore.getEventUpdates())
            .publish()

    var selectedNoteValue: Note? = null
//        private set

    val selectedNoteId: Subject<Optional<String>> = PublishSubject.create()

    val isSwitchingToNewlySelectedNote: Subject<Boolean> = PublishSubject.create()

    val selectedNote: ConnectableObservable<Optional<Note>> =
            selectedNoteId
                    .doOnNext { isSwitchingToNewlySelectedNote.onNext(true) }
                    .observeOn(Schedulers.io())
                    .switchMap { noteIdOptional ->
                        if (noteIdOptional.value != null) {
                            noteProjector.projectAndUpdate(noteIdOptional.value!!)
                                    .subscribeOn(Schedulers.io())
                                    .map { Optional(it) }
                        } else {
                            Observable.just(Optional())
                        }
                    }
                    .doOnNext { isSwitchingToNewlySelectedNote.onNext(false) }
                    .publish()

    init {
        selectedNote.subscribe { selectedNoteValue = it.value }
        selectedNoteId.subscribe { logger.info("Selected: ${it.value}") }
        selectedNote.subscribe { editingViewModel.nodeSelected(it) }
    }

    fun start() {
        selectedNote.connect()
        allEventsWithUpdates.connect()
        selectedNoteId.onNext(Optional())
    }

    // TODO use this
    sealed class Selection {
        object NoSelection : Selection()
        data class NoteSelection(val noteId: String, val title: String) : Selection()
        data class FolderSelection(val path: String, val title: String) : Selection()
    }
}