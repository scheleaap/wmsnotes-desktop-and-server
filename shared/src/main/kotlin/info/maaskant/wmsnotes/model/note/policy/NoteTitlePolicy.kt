package info.maaskant.wmsnotes.model.note.policy

import info.maaskant.wmsnotes.model.CommandBus
import info.maaskant.wmsnotes.model.CommandOrigin
import info.maaskant.wmsnotes.model.CommandOrigin.*
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.note.ChangeTitleCommand
import info.maaskant.wmsnotes.model.note.ContentChangedEvent
import info.maaskant.wmsnotes.model.note.NoteCommandRequest
import info.maaskant.wmsnotes.utilities.ApplicationService
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toObservable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteTitlePolicy @Inject constructor(
        private val commandBus: CommandBus,
        private val scheduler: Scheduler,
        private val titleExtractor: (content: String) -> String?
) : ApplicationService {

    private val logger by logger()

    private var disposable: Disposable? = null

    private fun connect(): Disposable {
        return commandBus.results
                .observeOn(scheduler)
                .filter { it.origin == LOCAL }
                .flatMap { it.newEvents.toObservable() }
                .filter { it is ContentChangedEvent }
                .map { it as ContentChangedEvent }
                .map { it to titleExtractor(it.content) }
                .filter { (_, title) -> title != null }
                .map { (event, title) ->
                    logger.debug("Changing title of aggregate {} to {}", event.aggId, title)
                    NoteCommandRequest.of(
                            command = ChangeTitleCommand(
                                    aggId = event.aggId,
                                    title = title!!
                            ),
                            lastRevision = event.revision,
                            origin = LOCAL
                    )
                }
                .subscribeBy(
                        onNext = commandBus.requests::onNext,
                        onError = { logger.error("Error", it) }
                )
    }

    @Synchronized
    override fun start() {
        if (disposable == null) {
            logger.debug("Starting")
            disposable = connect()
        }
    }

    @Synchronized
    override fun shutdown() {
        disposable?.let {
            logger.debug("Shutting down")
            it.dispose()
        }
        disposable = null
    }
}