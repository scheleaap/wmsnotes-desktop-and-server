package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.CommandHandler.Result.Handled
import info.maaskant.wmsnotes.model.CommandHandler.Result.NotHandled
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.folder.FolderCommand
import info.maaskant.wmsnotes.utilities.Optional
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AggregateCommandHandlerTest {
    private val aggId = "agg"

    private val repository: AggregateRepository<TestAggregate> = mockk()
    private val commandToEventMapper: CommandToEventMapper<TestAggregate> = mockk()

    private lateinit var handler: AggregateCommandHandler<TestAggregate>

    @BeforeEach
    fun init() {
        clearMocks(
                repository,
                commandToEventMapper
        )
        handler = AggregateCommandHandler(TestAggregateCommand::class, repository, commandToEventMapper)
    }

    @Test
    fun default() {
        // Given
        val command = TestAggregateCommand(aggId)
        val eventIn: Event = createEvent(aggId, 15)
        val eventOut: Event = createEvent(aggId, 15)
        val agg2 = TestAggregate(aggId = aggId, revision = 15)
        val agg1 = TestAggregate(aggId = aggId, revision = 14, application = eventIn to (agg2 to eventOut))
        every { commandToEventMapper.map(command, lastRevision = agg1.revision) }.returns(eventIn)
        every { repository.getLatest(aggId) }.returns(agg1)

        // When
        val result = handler.handle(command)

        // Then
        assertThat(result).isEqualTo(Handled(Optional(eventOut)))
    }

    @Test
    fun `no event returned by aggregate`() {
        // Given
        val command = TestAggregateCommand(aggId)
        val event1: Event = createEvent(aggId, 15)
        val agg1: TestAggregate = mockk()
        every { commandToEventMapper.map(command, lastRevision = 14) }.returns(event1)
        every { repository.getLatest(aggId) }.returns(agg1)
        every { agg1.revision }.returns(14)
        every { agg1.apply(event1) }.returns(agg1 to null)

        // When
        val result = handler.handle(command)

        // Then
        assertThat(result).isEqualTo(Handled(Optional()))
    }

    @Test
    fun `only handle commands that the handler is responsible for`() {
        // Given
        val command: FolderCommand = mockk()

        // When
        val result = handler.handle(command)

        // Then
        assertThat(result).isEqualTo(NotHandled)
    }

    private fun createEvent(aggId: String, revision: Int): Event {
        val event: Event = mockk()
        every { event.aggId }.returns(aggId)
        every { event.revision }.returns(revision)
        return event
    }

    private class TestAggregateCommand(aggId: String) : AggregateCommand(aggId)
    private data class TestAggregate(override val aggId: String, override val revision: Int, val application: Pair<Event, Pair<TestAggregate, Event>>? = null) : Aggregate<TestAggregate> {
        override fun apply(event: Event): Pair<TestAggregate, Event?> {
            return if (event == application?.first) {
                application.second
            } else {
                throw IllegalArgumentException(event.toString())
            }
        }
    }
}