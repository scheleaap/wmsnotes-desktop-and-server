package info.maaskant.wmsnotes.model.serialization

import info.maaskant.wmsnotes.model.AttachmentAddedEvent
import info.maaskant.wmsnotes.model.AttachmentDeletedEvent
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import info.maaskant.wmsnotes.utilities.serialization.KryoEventSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal class KryoEventSerializerTest {

    private val events = listOf(
            NoteCreatedEvent(eventId = 1, noteId = "note-1", revision = 1, title = "Title 1"),
            NoteDeletedEvent(eventId = 1, noteId = "note-1", revision = 1),
            AttachmentAddedEvent(eventId = 1, noteId = "note-1", revision = 1, name = "att-1", content = "DATA".toByteArray()),
            AttachmentDeletedEvent(eventId = 1, noteId = "note-1", revision = 1, name = "att-1")
            // Add more classes here
    )

    @TestFactory
    fun `serialization and deserialization`(): List<DynamicTest> {
        return events.map { eventBefore ->
            DynamicTest.dynamicTest(eventBefore::class.simpleName) {
                // Given
                val serializer = KryoEventSerializer()

                // When
                val eventAfter = serializer.deserialize(serializer.serialize(eventBefore))

                // Then
                assertThat(eventAfter).isEqualTo(eventBefore)
                assertThat(eventAfter.hashCode()).isEqualTo(eventBefore.hashCode())
            }
        }
    }

}