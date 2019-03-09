package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.CommandHandler.Result.*
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.utilities.Optional
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CommandProcessorTest {

    private val eventStore: EventStore = mockk()
    private val commandHandler1: CommandHandler = mockk()
    private val commandHandler2: CommandHandler = mockk()

    private lateinit var commandProcessor: CommandProcessor

    @BeforeEach
    fun init() {
        clearMocks(
                eventStore,
                commandHandler2
        )

        val eventSlot = slot<Event>()
        every { eventStore.appendEvent(capture(eventSlot)) }.answers { eventSlot.captured }
        every { commandHandler1.handle(any()) }.returns(NotHandled)
        every { commandHandler2.handle(any()) }.returns(NotHandled)
        commandProcessor = CommandProcessor(eventStore, commandHandler1, commandHandler2)
    }

    @Test
    fun `default, 1`() {
        // Given
        val command: Command = mockk()
        val event1: Event = createEvent("note", 15)
        val event2: Event = createEvent("note", 16)
        every { commandHandler1.handle(command) }.returns(Handled(Optional(event1)))
        every { commandHandler2.handle(command) }.returns(Handled(Optional(event2)))

        // When
        commandProcessor.blockingProcessCommand(command)

        // Then
        verify {
            eventStore.appendEvent(event1)
        }
    }

    @Test
    fun `default, 2`() {
        // Given
        val command: Command = mockk()
        val event: Event = createEvent("note", 16)
        every { commandHandler1.handle(command) }.returns(NotHandled)
        every { commandHandler2.handle(command) }.returns(Handled(Optional(event)))

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
        every { commandHandler2.handle(command) }.returns(Handled(Optional()))

        // When
        commandProcessor.blockingProcessCommand(command)

        // Then
        verify {
            eventStore.appendEvent(any()) wasNot Called
        }
    }

    @Test
    fun `command not handled`() {
        // Given
        val command: Command = mockk()

        // When / then
        assertThrows<IllegalArgumentException> {
            commandProcessor.blockingProcessCommand(command)
        }
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