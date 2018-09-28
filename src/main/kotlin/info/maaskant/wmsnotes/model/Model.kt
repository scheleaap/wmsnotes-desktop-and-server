package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.desktop.app.logger
import io.reactivex.ObservableTransformer
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Model @Inject constructor(private val eventStore: EventStore) {

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

        commands
                .compose(processCommands())
                .subscribe(events)

    }

    private fun processCommands(): ObservableTransformer<Command, Event> {
        return ObservableTransformer { it2 ->
            it2.doOnNext { logger.debug("Received command: $it") }
                    .map(this::executeCommand)
                    .compose(removeEmptyOptionalItems())
                    .observeOn(Schedulers.io())
                    .doOnNext { storeEvent(it) }
                    .doOnNext { logger.debug("Generated event: $it") }
        }
    }

    private fun <T> removeEmptyOptionalItems(): ObservableTransformer<Optional<T>, T> {
        return ObservableTransformer { it2 ->
            it2
                    .filter { it.isPresent }
                    .map { it.value }
        }
    }


    private fun storeEvent(e: Event) {
        eventStore.storeEvent(e).blockingGet()
    }

    private fun executeCommand(c: Command): Optional<Event> {
        return when (c) {
            is CreateNoteCommand -> Optional(createNote(c))
            is DeleteNoteCommand -> Optional(deleteNote(c))
            // else -> Optional.empty()
        }
    }

    private fun createNote(c: CreateNoteCommand): Event {
        return NoteCreatedEvent(eventId = 1, noteId = c.noteId, title = c.title)
    }

    private fun deleteNote(c: DeleteNoteCommand): Event {
        return NoteDeletedEvent(eventId = 1, noteId = c.noteId)
    }
}