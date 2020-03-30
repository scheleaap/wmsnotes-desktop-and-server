//package info.maaskant.wmsnotes.model.folder
//
//import info.maaskant.wmsnotes.model.*
//import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
//import info.maaskant.wmsnotes.model.eventstore.EventStore
//import io.mockk.mockk
//import io.reactivex.Scheduler
//
//internal class FolderCommandExecutorTest : CommandExecutorTest<Folder, FolderCommand, FolderCommandRequest, FolderCommandToEventMapper>() {
//    val path = Path("el")
//
//    override fun createMockedCommandToEventMapper(): FolderCommandToEventMapper =
//            mockk()
//
//    override fun createInstance(commandBus: CommandBus, eventStore: EventStore, repository: AggregateRepository<Folder>, commandToEventMapper: FolderCommandToEventMapper, scheduler: Scheduler): CommandExecutor<Folder, FolderCommand, FolderCommandRequest, FolderCommandToEventMapper> =
//            FolderCommandExecutor(commandBus, eventStore, repository, commandToEventMapper, scheduler)
//
//    override fun createMockedCommand(): FolderCommand = mockk()
//
//    override fun createCommandRequest(aggId: String, commands: List<FolderCommand>, lastRevision: Int?, requestId: Int, origin: CommandOrigin): FolderCommandRequest =
//            FolderCommandRequest(aggId, commands, lastRevision, requestId, origin)
//
//    override fun createEventThatChangesAggregate(agg: Folder): Triple<Event, Folder, Event> {
//        val eventIn = if (agg.exists) {
//            FolderDeletedEvent(eventId = 0, path = agg.path, revision = agg.revision + 1)
//        } else {
//            FolderCreatedEvent(eventId = 0, aggId = agg.aggId, path = agg.path, revision = agg.revision + 1)
//        }
//        val (new, eventOut) = agg.apply(eventIn)
//        return Triple(eventIn, new, eventOut!!)
//    }
//
//    override fun createEventThatDoesNotChangeAggregate(agg: Folder): Event =
//            if (agg.exists) {
//                FolderCreatedEvent(eventId = 0, aggId = agg.aggId, path = agg.path, revision = agg.revision + 1)
//            } else {
//                FolderDeletedEvent(eventId = 0, path = agg.path, revision = agg.revision + 1)
//            }
//
//    override fun getAggId1(): String {
//        return Folder.aggId(path)
//    }
//
//    override fun getInitialAggregate(aggId: String): Folder = Folder()
//            .apply(FolderCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = path)).component1()
//}