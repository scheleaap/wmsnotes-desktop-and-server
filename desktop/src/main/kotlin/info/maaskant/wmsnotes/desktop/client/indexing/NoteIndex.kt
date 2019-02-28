package info.maaskant.wmsnotes.desktop.client.indexing

import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import info.maaskant.wmsnotes.model.note.NoteDeletedEvent
import info.maaskant.wmsnotes.model.note.NoteUndeletedEvent
import info.maaskant.wmsnotes.utilities.logger
import info.maaskant.wmsnotes.utilities.persistence.StateProducer
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

class NoteIndex @Inject constructor(
        eventStore: EventStore,
        initialState: NoteIndexState?,
        scheduler: Scheduler
) : StateProducer<NoteIndexState> {

    private val logger by logger()

    private var state: NoteIndexState = initialState ?: NoteIndexState(isInitialized = false)
    private val stateUpdates: BehaviorSubject<NoteIndexState> = BehaviorSubject.create()

    init {
        var source = eventStore.getEventUpdates()
        if (!state.isInitialized) {
            source = Observable.concat(
                    eventStore.getEvents()
                            .doOnSubscribe { logger.debug("Creating initial note index") }
                            .doOnComplete {
                                updateState(state.initializationFinished())
                                logger.debug("Initial note index created")
                            },
                    source
            )
        }
        source
                .subscribeOn(scheduler)
                .subscribe({
                    when (it) {
                        is NoteCreatedEvent -> {
                            logger.debug("Adding note ${it.aggId} to index")
                            updateState(state.addNote(it.aggId, it.title))
                        }
                        is NoteDeletedEvent -> {
                            logger.debug("Hiding note ${it.aggId}")
                            updateState(state.hideNote(it.aggId))
                        }
                        is NoteUndeletedEvent -> {
                            logger.debug("Showing note ${it.aggId}")
                            updateState(state.showNote(it.aggId))
                        }
                        else -> {
                        }
                    }
                }, { logger.warn("Error", it) })
    }

    fun getNotes(): Observable<NoteMetadata> {
        return Observable.fromIterable(state.notes.values)
                .filter { !it.hidden }
    }

    private fun updateState(state: NoteIndexState) {
        this.state = state
        stateUpdates.onNext(state)
    }

    override fun getStateUpdates(): Observable<NoteIndexState> = stateUpdates

}

