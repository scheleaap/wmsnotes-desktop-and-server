package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.CommandHandler.Result.*
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.note.NoteCommandToEventMapper
import info.maaskant.wmsnotes.utilities.Optional
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AggregateCommandHandlerTest {

    private val repository: AggregateRepository<Note> = mockk()
    private val commandToEventMapper: NoteCommandToEventMapper = mockk()

    private lateinit var handler: AggregateCommandHandler<Note>

    @BeforeEach
    fun init() {
        clearMocks(
                repository,
                commandToEventMapper
        )
        handler = AggregateCommandHandler(repository, commandToEventMapper)
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
        every { repository.get("note", 14) }.returns(note1)
        every { note1.apply(event1) }.returns(note2 to event2)

        // When
        val result = handler.handle(command)

        // Then
        assertThat(result).isEqualTo(Handled(Optional(event2)))
    }

    @Test
    fun `no event returned by aggregate`() {
        // Given
        val command: Command = mockk()
        val event1: Event = createEvent("note", 15)
        val note1: Note = mockk()
        every { commandToEventMapper.map(command) }.returns(event1)
        every { repository.get("note", 14) }.returns(note1)
        every { note1.apply(event1) }.returns(note1 to null)

        // When
        val result = handler.handle(command)

        // Then
        assertThat(result).isEqualTo(Handled(Optional()))
    }

    private fun createEvent(aggId: String, revision: Int): Event {
        val event: Event = mockk()
        every { event.aggId }.returns(aggId)
        every { event.revision }.returns(revision)
        return event
    }

}