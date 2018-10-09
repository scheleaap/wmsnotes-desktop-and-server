package info.maaskant.wmsnotes.desktop.model

import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.NoteProjector
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

    var selectedNote: Note? = null
        private set

    val selectedNoteIdUpdates: Subject<String> = PublishSubject.create()

    val selectedNoteUpdates: ConnectableObservable<Note> = selectedNoteIdUpdates
            .subscribeOn(Schedulers.io())
            .map { noteProjector.project(it, null) } // TODO: Move observable to NoteProjector?
            .publish()

    init {
        selectedNoteUpdates.subscribe { selectedNote = it }
    }

    fun start() {
        selectedNoteUpdates.connect()
        allEventsWithUpdates.connect()
    }

}