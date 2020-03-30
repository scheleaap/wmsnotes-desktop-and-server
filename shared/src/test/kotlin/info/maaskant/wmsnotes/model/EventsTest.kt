package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderDeletedEvent
import info.maaskant.wmsnotes.model.note.*
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class EventsTest {
    private val aggId = "note"
    private val path = Path("el1", "el2")
    private val title = "Title"
    private val content = "Text"

    @Test
    fun `equals and hashCode for shared fields`() {
        val o = NoteDeletedEvent(eventId = 1, aggId = aggId, revision = 1)
        val different1 = NoteDeletedEvent(eventId = 2, aggId = o.aggId, revision = o.revision)
        val different2 = NoteDeletedEvent(eventId = o.eventId, aggId = "different", revision = o.revision)
        val different3 = NoteDeletedEvent(eventId = o.eventId, aggId = o.aggId, revision = 2)

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
    fun `copy with eventId`(): List<DynamicTest> {
        return listOf(
                NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = path, title = title, content = content),
                NoteDeletedEvent(eventId = 0, aggId = aggId, revision = 1),
                NoteUndeletedEvent(eventId = 0, aggId = aggId, revision = 1),
                AttachmentAddedEvent(eventId = 0, aggId = aggId, revision = 1, name = "att-1", content = "data".toByteArray()),
                AttachmentDeletedEvent(eventId = 0, aggId = aggId, revision = 1, name = "att-1"),
                ContentChangedEvent(eventId = 0, aggId = aggId, revision = 1, content = content),
                TitleChangedEvent(eventId = 0, aggId = aggId, revision = 1, title = title),
                MovedEvent(eventId = 0, aggId = aggId, revision = 1, path = path),
                FolderCreatedEvent(eventId = 0, revision = 1, path = path),
                FolderDeletedEvent(eventId = 0, revision = 1, path = path)
                // Add more classes here
        ).map {
            DynamicTest.dynamicTest(it::class.simpleName) {
                val copy = it.copy(eventId = 1)

                assertThat(copy.eventId).isEqualTo(1)
                assertThat(copy).isInstanceOf(it::class.java)
                assertThat(copy.copy(eventId = 0)).isEqualTo(it)
            }
        }
    }

    @TestFactory
    fun `copy with revision`(): List<DynamicTest> {
        return listOf(
                NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = path, title = title, content = content),
                NoteDeletedEvent(eventId = 0, aggId = aggId, revision = 1),
                NoteUndeletedEvent(eventId = 0, aggId = aggId, revision = 1),
                AttachmentAddedEvent(eventId = 0, aggId = aggId, revision = 1, name = "att-1", content = "data".toByteArray()),
                AttachmentDeletedEvent(eventId = 0, aggId = aggId, revision = 1, name = "att-1"),
                ContentChangedEvent(eventId = 0, aggId = aggId, revision = 1, content = "data"),
                TitleChangedEvent(eventId = 0, aggId = aggId, revision = 1, title = title),
                FolderCreatedEvent(eventId = 0, revision = 1, path = path),
                FolderDeletedEvent(eventId = 0, revision = 1, path = path)
                // Add more classes here
        ).map {
            DynamicTest.dynamicTest(it::class.simpleName) {
                val copy = it.copy(revision = 2)

                assertThat(copy.revision).isEqualTo(2)
                assertThat(copy).isInstanceOf(it::class.java)
                assertThat(copy.copy(revision = 1)).isEqualTo(it)
            }
        }
    }

    @TestFactory
    fun `equals and hashCode`(): List<DynamicTest> {
        return listOf(
                Item(
                        o = NoteCreatedEvent(eventId = 1, aggId = aggId, revision = 1, path = path, title = title, content = content),
                        sameButCopy = NoteCreatedEvent(eventId = 1, aggId = aggId, revision = 1, path = path, title = title, content = content),
                        differents = listOf(
                                NoteCreatedEvent(eventId = 1, aggId = aggId, revision = 1, path = Path("different"), title = title, content = content),
                                NoteCreatedEvent(eventId = 1, aggId = aggId, revision = 1, path = path, title = "Different", content = content),
                                NoteCreatedEvent(eventId = 1, aggId = aggId, revision = 1, path = path, title = title, content = "Different")
                        )
                ),
                Item(
                        o = NoteDeletedEvent(eventId = 1, aggId = aggId, revision = 1),
                        sameButCopy = NoteDeletedEvent(eventId = 1, aggId = aggId, revision = 1),
                        differents = listOf(NoteDeletedEvent(eventId = 1, aggId = "different", revision = 1))
                ),
                Item(
                        o = NoteUndeletedEvent(eventId = 1, aggId = aggId, revision = 1),
                        sameButCopy = NoteUndeletedEvent(eventId = 1, aggId = aggId, revision = 1),
                        differents = listOf(NoteUndeletedEvent(eventId = 1, aggId = "different", revision = 1))
                ),
                Item(
                        o = AttachmentAddedEvent(eventId = 1, aggId = aggId, revision = 1, name = "att-1", content = "data".toByteArray()),
                        sameButCopy = AttachmentAddedEvent(eventId = 1, aggId = aggId, revision = 1, name = "att-1", content = "data".toByteArray()),
                        differents = listOf(
                                AttachmentAddedEvent(eventId = 1, aggId = aggId, revision = 1, name = "different", content = "data".toByteArray()),
                                AttachmentAddedEvent(eventId = 1, aggId = aggId, revision = 1, name = "att-1", content = "different".toByteArray())
                        )
                ),
                Item(
                        o = AttachmentDeletedEvent(eventId = 1, aggId = aggId, revision = 1, name = "att-1"),
                        sameButCopy = AttachmentDeletedEvent(eventId = 1, aggId = aggId, revision = 1, name = "att-1"),
                        differents = listOf(AttachmentDeletedEvent(eventId = 1, aggId = aggId, revision = 1, name = "different"))
                ),
                Item(
                        o = ContentChangedEvent(eventId = 1, aggId = aggId, revision = 1, content = content),
                        sameButCopy = ContentChangedEvent(eventId = 1, aggId = aggId, revision = 1, content = content),
                        differents = listOf(ContentChangedEvent(eventId = 1, aggId = aggId, revision = 1, content = "Different"))
                ),
                Item(
                        o = TitleChangedEvent(eventId = 1, aggId = aggId, revision = 1, title = title),
                        sameButCopy = TitleChangedEvent(eventId = 1, aggId = aggId, revision = 1, title = title),
                        differents = listOf(TitleChangedEvent(eventId = 1, aggId = aggId, revision = 1, title = "Different"))
                ),
                Item(
                        o = MovedEvent(eventId = 1, aggId = aggId, revision = 1, path = path),
                        sameButCopy = MovedEvent(eventId = 1, aggId = aggId, revision = 1, path = path),
                        differents = listOf(MovedEvent(eventId = 1, aggId = aggId, revision = 1, path = Path("different")))
                ),
                Item(
                        o = FolderCreatedEvent(eventId = 1, revision = 1, path = path),
                        sameButCopy = FolderCreatedEvent(eventId = 1, revision = 1, path = path),
                        differents = listOf(FolderCreatedEvent(eventId = 1, revision = 1, path = Path("different")))
                ),
                Item(
                        o = FolderDeletedEvent(eventId = 1, revision = 1, path = path),
                        sameButCopy = FolderDeletedEvent(eventId = 1, revision = 1, path = path),
                        differents = listOf(FolderDeletedEvent(eventId = 1, revision = 1, path = Path("different")))
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

    @Test
    fun `equals for NoteDeletedEvent and NoteUndeletedEvent`() {
        // These two classes are identical with regards to their fields, but they still must never be equal.

        // Given
        val e1 = NoteDeletedEvent(eventId = 0, aggId = aggId, revision = 1)
        val e2 = NoteUndeletedEvent(eventId = 0, aggId = aggId, revision = 1)

        // When / then
        assertThat(e1).isNotEqualTo(e2)
    }

    private data class Item(val o: Event, val sameButCopy: Event, val differents: List<Event>)
}
