package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.desktop.app.logger
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandProcessor @Inject constructor(private val eventStore: EventStore) {

    private val logger by logger()

    val commands: Subject<Command> = PublishSubject.create()
    val events: Subject<Event> = PublishSubject.create() // TODO: Move to EventStore

    init {
        commands
                .compose(processCommands())
                .compose(removeEmptyOptionalItems())
                .subscribe(events)
    }

    @Synchronized
    fun blockingProcessCommand(command: Command, lastEventId: Int?): Event? { // TODO: Write test for lastEventId
        return Observable
                .just(command)
                .compose(processCommands())
                .blockingSingle()
                .value
    }

    private fun processCommands(): ObservableTransformer<Command, Optional<Event>> {
        return ObservableTransformer { it2 ->
            it2.doOnNext { logger.debug("Received command: $it") }
                    .map(this::executeCommand)
                    .observeOn(Schedulers.io())
                    .doOnNext { storeEventIfPresent(it) }
                    .doOnNext { logEventIfPresent(it) } // TODO: Move to EventStore
        }
    }

    private fun logEventIfPresent(e: Optional<Event>) {
        if (e.value != null) {
            logger.debug("Generated event: $e.value")
        }
    }

    private fun storeEventIfPresent(e: Optional<Event>) {
        if (e.value != null) {
            logger.debug("Storing event: $e.value")
            eventStore.appendEvent(e.value)
        }
    }

    private fun <T> removeEmptyOptionalItems(): ObservableTransformer<Optional<T>, T> {
        return ObservableTransformer { it2 ->
            it2
                    .filter { it.isPresent }
                    .map { it.value }
        }
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