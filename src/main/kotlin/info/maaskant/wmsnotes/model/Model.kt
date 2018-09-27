package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.desktop.app.logger
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.*

class Model(private val eventStore: EventStore) {

    private val logger by logger()

    val commands: Subject<Command> = PublishSubject.create()
    //    val events: Observable<Event> =
//            Observable.just(
//                    NoteCreatedEvent("1", "Note 1"),
//                    NoteCreatedEvent("2", "Note 2"),
//                    NoteDeletedEvent("2"),
//                    NoteCreatedEvent("3", "Note 3")
//            )
    val events: Subject<Event> = PublishSubject.create()

    init {

        commands.doOnNext { logger.debug("Received command: $it") }
                .map(this::processCommand)
                .filter(Optional<Event>::isPresent)
                .map(Optional<Event>::get)
//                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnNext { logger.debug("Storing event: $it") }
                .doOnNext { store(it) }
                .doOnNext { logger.debug("Emitting event: $it") }
                .subscribe(events)

    }

    private fun store(e: Event) {
        eventStore
                .storeEvent(e)
//                .concatWith { eventStore.getEvent(e.eventId) }
                .subscribe()
    }

    private fun processCommand(c: Command): Optional<Event> {
        return when (c) {
            is CreateNoteCommand -> Optional.of(createNote(c))
            is DeleteNoteCommand -> Optional.of(deleteNote(c))
            // else -> Optional.empty()
        }
    }

    private fun createNote(c: CreateNoteCommand): Event {
        return NoteCreatedEvent(c.id, c.title)
    }

    private fun deleteNote(c: DeleteNoteCommand): Event {
        return NoteDeletedEvent(c.id)
    }
}