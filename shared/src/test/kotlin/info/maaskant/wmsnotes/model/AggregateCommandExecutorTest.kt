package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.CommandRequest.Companion.randomRequestId
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("LocalVariableName")
internal abstract class AggregateCommandExecutorTest<
        AggregateType : Aggregate<AggregateType>,
        CommandType : AggregateCommand,
        CommandRequestType : AggregateCommandRequest<CommandType>,
        MapperType : CommandToEventMapper<AggregateType>
        > {
    private val eventStore: EventStore = mockk()
    private val repository: AggregateRepository<AggregateType> = mockk()
    private lateinit var commandToEventMapper: MapperType

    private lateinit var executor: AggregateCommandExecutor<AggregateType, CommandType, CommandRequestType, MapperType>

    @BeforeEach
    fun init() {
        clearMocks(
                eventStore,
                repository
        )
        commandToEventMapper = createMockedCommandToEventMapper()
        executor = createInstance(eventStore, repository, commandToEventMapper)
    }

    @Test
    fun `execute, one command, with event produced`() {
        // Given
        val aggId = getAggId1()
        val command1 = createMockedCommand()
        val request = createCommandRequest(aggId, listOf(command1), lastRevision = null, requestId = randomRequestId())
        val agg1 = getInitialAggregate(aggId)
        val (event1In, _, event1Out) = createEventThatChangesAggregate(agg1)
        givenAStoredAggregate(aggId, agg1)
        givenACommandCanBeMappedToAnEvent(command = command1, lastRevision = agg1.revision, event = event1In)
        givenAnEventCanBeStored(eventIn = event1Out)

        // When
        val result = executor.execute(request)

        // Then
        assertThat(result).isEqualTo(CommandResult.Success(request.requestId))
        verifySequence { eventStore.appendEvent(event1Out) }
    }

    @Test
    fun `execute, one command, without event produced`() {
        // Given
        val aggId = getAggId1()
        val command1 = createMockedCommand()
        val request = createCommandRequest(aggId, listOf(command1), lastRevision = null, requestId = randomRequestId())
        val agg1 = getInitialAggregate(aggId)
        val event1In = createEventThatDoesNotChangeAggregate(agg1)
        givenAStoredAggregate(aggId, agg1)
        givenACommandCanBeMappedToAnEvent(command = command1, lastRevision = agg1.revision, event = event1In)
        givenStoringAnEventFails()

        // When
        val result = executor.execute(request)

        // Then
        assertThat(result).isEqualTo(CommandResult.Success(request.requestId))
        verify { eventStore.appendEvent(any()) wasNot Called }
    }

    @Test
    fun `execute, multiple commands`() {
        // Given
        val aggId = getAggId1()
        val command1 = createMockedCommand()
        val command2 = createMockedCommand()
        val request = createCommandRequest(aggId, listOf(command1, command2), lastRevision = null, requestId = randomRequestId())
        val agg1 = getInitialAggregate(aggId)
        val (event1In, agg2, event1Out) = createEventThatChangesAggregate(agg1)
        val (event2In, _, event2Out) = createEventThatChangesAggregate(agg2)
        givenAStoredAggregate(aggId, agg1)
        givenACommandCanBeMappedToAnEvent(command = command1, lastRevision = agg1.revision, event = event1In)
        givenACommandCanBeMappedToAnEvent(command = command2, lastRevision = agg2.revision, event = event2In)
        givenAnEventCanBeStored(eventIn = event1Out)
        givenAnEventCanBeStored(eventIn = event2Out)

        // When
        val result = executor.execute(request)

        // Then
        assertThat(result).isEqualTo(CommandResult.Success(request.requestId))
        verifySequence {
            eventStore.appendEvent(event1Out)
            eventStore.appendEvent(event2Out)
        }
    }

    @Test
    fun `execute, multiple commands, first one fails`() {
        // Given
        val aggId = getAggId1()
        val command1 = createMockedCommand()
        val command2 = createMockedCommand()
        val request = createCommandRequest(aggId, listOf(command1, command2), lastRevision = null, requestId = randomRequestId())
        val agg1 = getInitialAggregate(aggId)
        val (event1In, agg2, event1Out) = createEventThatChangesAggregate(agg1)
        val (event2In, _, event2Out) = createEventThatChangesAggregate(agg2)
        givenAStoredAggregate(aggId, agg1)
        givenACommandCanBeMappedToAnEvent(command = command1, lastRevision = agg1.revision, event = event1In)
        givenACommandCanBeMappedToAnEvent(command = command2, lastRevision = agg2.revision, event = event2In)
        givenStoringAnEventFails(event = event1Out)
        givenAnEventCanBeStored(eventIn = event2Out)

        // When
        val result = executor.execute(request)

        // Then
        assertThat(result).isEqualTo(CommandResult.Failure(request.requestId))
        verifySequence { eventStore.appendEvent(event1Out) }
    }

    @Test
    fun `execute, wrong lastRevision specified in request`() {
        // Given
        val aggId = getAggId1()
        val command1 = createMockedCommand()
        val request = createCommandRequest(aggId, listOf(command1), lastRevision = 15, requestId = randomRequestId())
        val agg1 = getInitialAggregate(aggId)
        val (event1In, _, event1Out) = createEventThatChangesAggregate(agg1, lastRevision = 15)
        givenAStoredAggregate(aggId, agg1)
        givenACommandCanBeMappedToAnEvent(command = command1, lastRevision = 15, event = event1In)
        givenStoringAnEventFails(event = event1Out) // Because revisions do not match

        // When
        val result = executor.execute(request)

        // Then
        assertThat(result).isEqualTo(CommandResult.Failure(request.requestId))
        verify { eventStore.appendEvent(any()) wasNot Called }
    }

//    @Disabled @Test
//    fun `only handle commands that the handler is responsible for`() {
//        // Given
//        val command: FolderCommand = mockk()
//
//        // When
//        val result = executor.handle(command)
//
//        // Then
//        assertThat(result).isEqualTo(NotHandled)
//    }

    private fun givenAnEventCanBeStored(eventIn: Event) {
        val eventOut: Event = mockk()
        every { eventStore.appendEvent(eventIn) }.returns(eventOut)
    }

    private fun givenStoringAnEventFails() {
        every { eventStore.appendEvent(any()) }.throws(IllegalArgumentException())
    }

    private fun givenStoringAnEventFails(event: Event) {
        every { eventStore.appendEvent(event) }.throws(IllegalArgumentException())
    }

    private fun givenACommandCanBeMappedToAnEvent(command: CommandType, lastRevision: Int, event: Event) {
        every { commandToEventMapper.map(command, lastRevision = lastRevision) }.returns(event)
    }

    private fun givenAStoredAggregate(aggId: String, agg: AggregateType) {
        every { repository.getLatest(aggId) }.returns(agg)
    }

    protected abstract fun createMockedCommandToEventMapper(): MapperType
    protected abstract fun createInstance(eventStore: EventStore, repository: AggregateRepository<AggregateType>, commandToEventMapper: MapperType): AggregateCommandExecutor<AggregateType, CommandType, CommandRequestType, MapperType>
    protected abstract fun createMockedCommand(): CommandType
    protected abstract fun createCommandRequest(aggId: String, commands: List<CommandType>, lastRevision: Int?, requestId: Int): CommandRequestType
    protected abstract fun createEventThatChangesAggregate(agg: AggregateType): Triple<Event, AggregateType, Event>
    private fun createEventThatChangesAggregate(agg: AggregateType, lastRevision: Int): Triple<Event, AggregateType, Event> {
        val (eventIn, aggregate, eventOut) = createEventThatChangesAggregate(agg)
        return Triple(eventIn.copy(revision = lastRevision), aggregate, eventOut)
    }

    protected abstract fun createEventThatDoesNotChangeAggregate(agg: AggregateType): Event
    protected abstract fun getInitialAggregate(aggId: String): AggregateType
    protected abstract fun getAggId1(): String
}