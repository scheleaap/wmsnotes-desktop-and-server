package info.maaskant.wmsnotes.model.folder

import info.maaskant.wmsnotes.model.AbstractCommandExecutor
import info.maaskant.wmsnotes.model.CommandBus
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import io.reactivex.Scheduler
import javax.inject.Inject

class FolderCommandExecutor @Inject constructor(
        commandBus: CommandBus,
        eventStore: EventStore,
        repository: AggregateRepository<Folder>,
        commandToEventMapper: FolderCommandToEventMapper,
        scheduler: Scheduler
) : AbstractCommandExecutor<Folder, FolderCommand, FolderCommandRequest, FolderCommandToEventMapper>(FolderCommandRequest::class, commandBus, eventStore, repository, commandToEventMapper, scheduler)
