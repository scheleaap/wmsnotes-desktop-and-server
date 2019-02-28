package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.utilities.Optional
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandProcessor @Inject constructor(
        private val eventStore: EventStore,
        private val commandHandler: CommandHandler
) {

    private val logger by logger()

    val commands: Subject<Command> = PublishSubject.create()

    init {
        commands
                .compose(processCommands())
                .subscribe({}, { logger.warn("Error", it) })
    }

    @Synchronized
    fun blockingProcessCommand(command: Command): Event? {
        return Observable
                .just(command)
                .compose(processCommands())
                .blockingSingle()
                .value
    }

    private fun processCommands(): ObservableTransformer<Command, Optional<Event>> {
        return ObservableTransformer { it2 ->
            it2
                    .observeOn(Schedulers.io())
                    .doOnNext { logger.debug("Received command: $it") }
                    .map(this::handleCommand)
                    .map { storeEventIfPresent(it) }
        }
    }

    private fun storeEventIfPresent(e: Optional<Event>): Optional<Event> {
        if (e.value != null) {
            logger.debug("Storing event: ${e.value}")
            return Optional(eventStore.appendEvent(e.value))
        } else {
            return e
        }
    }

    private fun <T> removeEmptyOptionalItems(): ObservableTransformer<Optional<T>, T> {
        return ObservableTransformer { it2 ->
            it2
                    .filter { it.isPresent }
                    .map { it.value }
        }
    }

    private fun handleCommand(command: Command): Optional<Event> {
        return commandHandler.handle(command)
    }
}