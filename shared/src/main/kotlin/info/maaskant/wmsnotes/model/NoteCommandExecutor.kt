package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.model.note.NoteCommand
import info.maaskant.wmsnotes.model.note.NoteCommandToEventMapper
import javax.inject.Inject

class NoteCommandExecutor @Inject constructor(
        eventStore: EventStore,
        repository: AggregateRepository<Note>,
        commandToEventMapper: NoteCommandToEventMapper
) : AggregateCommandExecutor<Note, NoteCommand, NoteCommandRequest, NoteCommandToEventMapper>(eventStore, repository, commandToEventMapper)
