package info.maaskant.wmsnotes.model.serialization

import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal class KryoEventSerializerTest {

    private val events = listOf(
            NoteCreatedEvent(1, "note-1", "Title 1"),
            NoteDeletedEvent(1, "note-1")
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