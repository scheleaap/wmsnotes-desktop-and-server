package info.maaskant.wmsnotes.model.note

import info.maaskant.wmsnotes.model.AggregateCommandExecutor
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import javax.inject.Inject

class NoteCommandExecutor @Inject constructor(
        eventStore: EventStore,
        repository: AggregateRepository<Note>,
        commandToEventMapper: NoteCommandToEventMapper
) : AggregateCommandExecutor<Note, NoteCommand, NoteCommandRequest, NoteCommandToEventMapper>(NoteCommandRequest::class, eventStore, repository, commandToEventMapper)
