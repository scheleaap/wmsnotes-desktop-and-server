package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.NoteProjector
import io.mockk.*
import org.junit.jupiter.api.*

internal class CommandProcessorTest {

    private val eventStore: EventStore = mockk()
    private val projector: NoteProjector = mockk()
    private val commandToEventMapper: CommandToEventMapper = mockk()

    private lateinit var commandProcessor: CommandProcessor

    @BeforeEach
    fun init() {
        clearMocks(
                eventStore,
                projector,
                commandToEventMapper
        )

        val eventSlot = slot<Event>()
        every { eventStore.appendEvent(capture(eventSlot)) }.answers { eventSlot.captured }

        commandProcessor = CommandProcessor(eventStore, projector, commandToEventMapper)
    }

    @Test
    fun default() {
        // Given
        val command: Command = mockk()
        val event1: Event = createEvent("note", 15)
        val note1: Note = mockk()
        val event2: Event = createEvent("note", 15)
        val note2: Note = mockk()
        every { commandToEventMapper.map(command) }.returns(event1)
        every { projector.project("note", 14) }.returns(note1)
        every { note1.apply(event1) }.returns(note2 to event2)

        // When
        commandProcessor.blockingProcessCommand(command)

        // Then
        verify {
            eventStore.appendEvent(event2)
        }
    }

    @Test
    fun `no event returned by aggregate`() {
        // Given
        val command: Command = mockk()
        val event1: Event = createEvent("note", 15)
        val note1: Note = mockk()
        every { commandToEventMapper.map(command) }.returns(event1)
        every { projector.project("note", 14) }.returns(note1)
        every { note1.apply(event1) }.returns(note1 to null)

        // When
        commandProcessor.blockingProcessCommand(command)

        // Then
        verify {
            eventStore.appendEvent(any()) wasNot Called
        }
    }

    private fun createEvent(noteId: String, revision: Int): Event {
        val event: Event = mockk()
        every { event.noteId }.returns(noteId)
        every { event.revision }.returns(revision)
        return event
    }

}