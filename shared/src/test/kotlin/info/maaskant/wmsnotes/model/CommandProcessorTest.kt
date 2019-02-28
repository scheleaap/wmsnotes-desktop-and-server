package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.utilities.Optional
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CommandProcessorTest {

    private val eventStore: EventStore = mockk()
    private val commandHandler: CommandHandler = mockk()

    private lateinit var commandProcessor: CommandProcessor

    @BeforeEach
    fun init() {
        clearMocks(
                eventStore,
                commandHandler
        )

        val eventSlot = slot<Event>()
        every { eventStore.appendEvent(capture(eventSlot)) }.answers { eventSlot.captured }

        commandProcessor = CommandProcessor(eventStore, commandHandler)
    }

    @Test
    fun default() {
        // Given
        val command: Command = mockk()
        val event: Event = createEvent("note", 15)
        every { commandHandler.handle(command) }.returns(Optional(event))

        // When
        commandProcessor.blockingProcessCommand(command)

        // Then
        verify {
            eventStore.appendEvent(event)
        }
    }

    @Test
    fun `no event returned by aggregate`() {
        // Given
        val command: Command = mockk()
        every { commandHandler.handle(command) }.returns(Optional())

        // When
        commandProcessor.blockingProcessCommand(command)

        // Then
        verify {
            eventStore.appendEvent(any()) wasNot Called
        }
    }

    private fun createEvent(aggId: String, revision: Int): Event {
        val event: Event = mockk()
        every { event.aggId }.returns(aggId)
        every { event.revision }.returns(revision)
        return event
    }

}