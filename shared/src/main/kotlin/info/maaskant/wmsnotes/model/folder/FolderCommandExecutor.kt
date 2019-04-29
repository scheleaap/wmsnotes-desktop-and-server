package info.maaskant.wmsnotes.model.folder

import info.maaskant.wmsnotes.model.AbstractCommandExecutor
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import javax.inject.Inject

class FolderCommandExecutor @Inject constructor(
        eventStore: EventStore,
        repository: AggregateRepository<Folder>,
        commandToEventMapper: FolderCommandToEventMapper
) : AbstractCommandExecutor<Folder, FolderCommand, FolderCommandRequest, FolderCommandToEventMapper>(FolderCommandRequest::class, eventStore, repository, commandToEventMapper)
