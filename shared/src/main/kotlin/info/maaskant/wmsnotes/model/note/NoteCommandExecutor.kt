package info.maaskant.wmsnotes.model.note

import info.maaskant.wmsnotes.model.AbstractCommandExecutor
import info.maaskant.wmsnotes.model.CommandBus
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteCommandExecutor @Inject constructor(
        commandBus: CommandBus,
        eventStore: EventStore,
        repository: AggregateRepository<Note>,
        commandToEventMapper: NoteCommandToEventMapper,
        scheduler: Scheduler
) : AbstractCommandExecutor<Note, NoteCommand, NoteCommandRequest, NoteCommandToEventMapper>(NoteCommandRequest::class, commandBus, eventStore, repository, commandToEventMapper, scheduler)
