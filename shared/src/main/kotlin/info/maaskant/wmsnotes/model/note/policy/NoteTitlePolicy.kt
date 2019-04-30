package info.maaskant.wmsnotes.model.note.policy

import info.maaskant.wmsnotes.model.CommandBus
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.note.ChangeTitleCommand
import info.maaskant.wmsnotes.model.note.ContentChangedEvent
import info.maaskant.wmsnotes.model.note.NoteCommandRequest
import info.maaskant.wmsnotes.utilities.ApplicationService
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy

class NoteTitlePolicy(
        private val commandBus: CommandBus,
        private val eventStore: EventStore,
        private val scheduler: Scheduler,
        private val titleExtractor: (content: String) -> String?
) : ApplicationService {

    private val logger by logger()

    private var disposable: Disposable? = null

    private fun connect(): Disposable {
        return eventStore.getEventUpdates()
                .observeOn(scheduler)
                .filter { it is ContentChangedEvent }
                .map { it as ContentChangedEvent }
                .map { it to titleExtractor(it.content) }
                .filter { (_, title) -> title != null }
                .map { (event, title) ->
                    NoteCommandRequest.of(
                            command = ChangeTitleCommand(
                                    aggId = event.aggId,
                                    title = title!!
                            ),
                            lastRevision = event.revision
                    )
                }
                .subscribeBy(
                        onNext = commandBus.requests::onNext,
                        onError = { logger.warn("Error", it) }
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