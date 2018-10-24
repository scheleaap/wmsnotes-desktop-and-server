package info.maaskant.wmsnotes.model.serialization

import info.maaskant.wmsnotes.model.AttachmentAddedEvent
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.cache.KryoNoteSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal class KryoNoteSerializerTest {

    private val noteId = "note"
    private val notes = listOf(
            Note()
                    .apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = "Title")).component1()
                    .apply(AttachmentAddedEvent(eventId = 2, noteId = noteId, revision = 2, name = "att-1", content = "data1".toByteArray())).component1()
                    .apply(AttachmentAddedEvent(eventId = 3, noteId = noteId, revision = 3, name = "att-2", content = "data2".toByteArray())).component1()
    )

    @TestFactory
    fun `serialization and deserialization`(): List<DynamicTest> {
        return notes.map { noteBefore ->
            DynamicTest.dynamicTest(noteBefore.toString()) {
                // Given
                val serializer = KryoNoteSerializer()

                // When
                val noteAfter = serializer.deserialize(serializer.serialize(noteBefore))

                // Then
                assertThat(noteAfter).isEqualTo(noteBefore)
                assertThat(noteAfter.hashCode()).isEqualTo(noteBefore.hashCode())
            }
        }
    }

}