package info.maaskant.wmsnotes.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class EventsTest {
    @Test
    fun `equals and hashCode for shared fields`() {
        val o = NoteDeletedEvent(eventId = 1, noteId = "note-1", revision = 1)
        val different1 = NoteDeletedEvent(eventId = 2, noteId = o.noteId, revision = o.revision)
        val different2 = NoteDeletedEvent(eventId = o.eventId, noteId = "note-2", revision = o.revision)
        val different3 = NoteDeletedEvent(eventId = o.eventId, noteId = o.noteId, revision = 2)

        assertThat(o).isEqualTo(o)
        assertThat(o.hashCode()).isEqualTo(o.hashCode())
        assertThat(o).isNotEqualTo(different1)
        assertThat(o.hashCode()).isNotEqualTo(different1.hashCode())
        assertThat(o).isNotEqualTo(different2)
        assertThat(o.hashCode()).isNotEqualTo(different2.hashCode())
        assertThat(o).isNotEqualTo(different3)
        assertThat(o.hashCode()).isNotEqualTo(different3.hashCode())
    }

    @TestFactory
    fun `equals and hashCode for all event types`(): List<DynamicTest> {
        return listOf(
                Item(
                        o = NoteCreatedEvent(eventId = 1, noteId = "note-1", revision = 1, title = "Title 1"),
                        sameButCopy = NoteCreatedEvent(eventId = 1, noteId = "note-1", revision = 1, title = "Title 1"),
                        different = NoteCreatedEvent(eventId = 1, noteId = "note-1", revision = 1, title = "Title 2")
                ),
                Item(
                        o = NoteDeletedEvent(eventId = 1, noteId = "note-1", revision = 1),
                        sameButCopy = NoteDeletedEvent(eventId = 1, noteId = "note-1", revision = 1),
                        different = NoteDeletedEvent(eventId = 1, noteId = "note-2", revision = 1)
                )
        )
                .map {
                    DynamicTest.dynamicTest(it.o::class.simpleName) {
                        assertThat(it.o).isEqualTo(it.o)
                        assertThat(it.o.hashCode()).isEqualTo(it.o.hashCode())
                        assertThat(it.o).isEqualTo(it.sameButCopy)
                        assertThat(it.o.hashCode()).isEqualTo(it.sameButCopy.hashCode())
                        assertThat(it.o).isNotEqualTo(it.different)
                        assertThat(it.o.hashCode()).isNotEqualTo(it.different.hashCode())
                        println(it.o)
                    }
                }
    }
}

private data class Item(val o: Event, val sameButCopy: Event, val different: Event)
