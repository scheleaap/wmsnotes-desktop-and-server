package info.maaskant.wmsnotes.desktop.model

import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.utilities.Optional
import io.reactivex.Observable
import io.reactivex.observables.ConnectableObservable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationModel @Inject constructor(val eventStore: EventStore, val noteProjector: NoteProjector) {

    private val logger by logger()

    val allEventsWithUpdates: ConnectableObservable<Event> = eventStore.getEvents()
            .subscribeOn(Schedulers.io())
            .mergeWith(eventStore.getEventUpdates())
            .publish()

    var selectedNoteValue: Note? = null
        private set

    val selectedNoteId: Subject<Optional<String>> = PublishSubject.create()

    val isSwitchingToNewlySelectedNote: Subject<Boolean> = PublishSubject.create()

    val selectedNote: ConnectableObservable<Optional<Note>> =
            Observable.merge(
                    selectedNoteId
                            .doOnNext { isSwitchingToNewlySelectedNote.onNext(true) },
                    allEventsWithUpdates
                            .filter { it.noteId == selectedNoteValue?.noteId }
                            .map { Optional(it.noteId) }
            )
                    .observeOn(Schedulers.io())
                    .switchMap {
                        Observable.just(it.map {
                            noteProjector.project(it, null)
                        }).subscribeOn(Schedulers.io())
                    }
                    .doOnNext { isSwitchingToNewlySelectedNote.onNext(false) }
                    .publish()

    init {
        selectedNote.subscribe { selectedNoteValue = it.value }
        selectedNoteId.subscribe { logger.info("Selected: ${it.value}") }
    }

    fun start() {
        selectedNote.connect()
        allEventsWithUpdates.connect()
        selectedNoteId.onNext(Optional())
    }
}