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
    fun `withEventId for all event types`(): List<DynamicTest> {
        return listOf(
                NoteCreatedEvent(eventId = 0, noteId = "note-1", revision = 1, title = "Title 1"),
                NoteDeletedEvent(eventId = 0, noteId = "note-1", revision = 1),
                AttachmentAddedEvent(eventId = 0, noteId = "note-1", revision = 1, name = "att-1", content = "DATA".toByteArray()),
                AttachmentDeletedEvent(eventId = 0, noteId = "note-1", revision = 1, name = "att-1")
                // Add more classes here
        ).map {
            DynamicTest.dynamicTest(it::class.simpleName) {
                val copy = it.withEventId(eventId = 1)

                assertThat(copy.eventId).isEqualTo(1)
                assertThat(copy).isInstanceOf(it::class.java)
                assertThat(copy.withEventId(0)).isEqualTo(it)
            }
        }
    }

    @TestFactory
    fun `equals and hashCode for all event types`(): List<DynamicTest> {
        return listOf(
                Item(
                        o = NoteCreatedEvent(eventId = 1, noteId = "note-1", revision = 1, title = "Title 1"),
                        sameButCopy = NoteCreatedEvent(eventId = 1, noteId = "note-1", revision = 1, title = "Title 1"),
                        differents = listOf(NoteCreatedEvent(eventId = 1, noteId = "note-1", revision = 1, title = "Title 2"))
                ),
                Item(
                        o = NoteDeletedEvent(eventId = 1, noteId = "note-1", revision = 1),
                        sameButCopy = NoteDeletedEvent(eventId = 1, noteId = "note-1", revision = 1),
                        differents = listOf(NoteDeletedEvent(eventId = 1, noteId = "note-2", revision = 1))
                ),
                Item(
                        o = AttachmentAddedEvent(eventId = 1, noteId = "note-1", revision = 1, name = "att-1", content = "DATA".toByteArray()),
                        sameButCopy = AttachmentAddedEvent(eventId = 1, noteId = "note-1", revision = 1, name = "att-1", content = "DATA".toByteArray()),
                        differents = listOf(
                                AttachmentAddedEvent(eventId = 1, noteId = "note-1", revision = 1, name = "att-2", content = "DATA".toByteArray()),
                                AttachmentAddedEvent(eventId = 1, noteId = "note-1", revision = 1, name = "att-1", content = "DIFFERENT".toByteArray())
                        )
                ),
                Item(
                        o = AttachmentDeletedEvent(eventId = 1, noteId = "note-1", revision = 1, name = "att-1"),
                        sameButCopy = AttachmentDeletedEvent(eventId = 1, noteId = "note-1", revision = 1, name = "att-1"),
                        differents = listOf(AttachmentDeletedEvent(eventId = 1, noteId = "note-1", revision = 1, name = "att-2"))
                )
                // Add more classes here
        ).map {
            DynamicTest.dynamicTest(it.o::class.simpleName) {
                assertThat(it.o).isEqualTo(it.o)
                assertThat(it.o.hashCode()).isEqualTo(it.o.hashCode())
                assertThat(it.o).isEqualTo(it.sameButCopy)
                assertThat(it.o.hashCode()).isEqualTo(it.sameButCopy.hashCode())
                for (different in it.differents) {
                    assertThat(it.o).isNotEqualTo(different)
                    assertThat(it.o.hashCode()).isNotEqualTo(different.hashCode())
                }
            }
        }
    }
}

private data class Item(val o: Event, val sameButCopy: Event, val differents: List<Event>)