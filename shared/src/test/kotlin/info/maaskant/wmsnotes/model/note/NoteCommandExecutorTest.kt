package info.maaskant.wmsnotes.model.note

import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.CommandExecutorTest
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import io.mockk.mockk

internal class NoteCommandExecutorTest : CommandExecutorTest<Note, NoteCommand, NoteCommandRequest, NoteCommandToEventMapper>() {
    override fun createMockedCommandToEventMapper(): NoteCommandToEventMapper =
            mockk()

    override fun createInstance(eventStore: EventStore, repository: AggregateRepository<Note>, commandToEventMapper: NoteCommandToEventMapper): CommandExecutor<Note, NoteCommand, NoteCommandRequest, NoteCommandToEventMapper> =
            NoteCommandExecutor(eventStore, repository, commandToEventMapper)

    override fun createMockedCommand(): NoteCommand = mockk()

    override fun createCommandRequest(aggId: String, commands: List<NoteCommand>, lastRevision: Int?, requestId: Int): NoteCommandRequest =
            NoteCommandRequest(aggId, commands, lastRevision, requestId)

    override fun createEventThatChangesAggregate(agg: Note): Triple<Event, Note, Event> {
        val eventIn = TitleChangedEvent(eventId = 0, aggId = agg.aggId, revision = agg.revision + 1, title = agg.title + "+")
        val (new, eventOut) = agg.apply(eventIn)
        return Triple(eventIn, new, eventOut!!)
    }

    override fun createEventThatDoesNotChangeAggregate(agg: Note): Event =
            TitleChangedEvent(eventId = 0, aggId = agg.aggId, revision = agg.revision + 1, title = agg.title)

    override fun getAggId1(): String = "n-10000000-0000-0000-0000-000000000000"

    override fun getInitialAggregate(aggId: String): Note = Note()
            .apply(NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = Path(), title = "Title", content = "Content")).component1()
}