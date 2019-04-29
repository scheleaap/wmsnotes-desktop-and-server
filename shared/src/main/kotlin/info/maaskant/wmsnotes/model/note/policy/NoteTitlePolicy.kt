package info.maaskant.wmsnotes.model.note.policy

import info.maaskant.wmsnotes.model.CommandBus
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.note.ChangeTitleCommand
import info.maaskant.wmsnotes.model.note.ContentChangedEvent
import info.maaskant.wmsnotes.model.note.NoteCommandRequest
import io.reactivex.Scheduler

class NoteTitlePolicy(
        private val commandBus: CommandBus,
        private val eventStore: EventStore,
        private val scheduler: Scheduler,
        private val titleExtractor: (content: String) -> String?
) {
    fun start() {
        eventStore.getEventUpdates()
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
                .subscribe(commandBus.requests)
    }
}