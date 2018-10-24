package info.maaskant.wmsnotes.client.indexing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mapdb.Atomic
import org.mapdb.DBMaker

internal class MapDbNoteMetadataSerializerTest {
    private val noteId = "note"

    @Test
    fun `serialization and deserialization`() {
        // Given
        val variable: Atomic.Var<NoteMetadata> = DBMaker.memoryDB().make().atomicVar("item", MapDbNoteMetadataSerializer()).createOrOpen()
        val before = NoteMetadata(noteId = noteId, title = "Title!")

        // When
        variable.set(before)
        val after = variable.get()

        // Then
        assertThat(after).isEqualTo(before)
        assertThat(after.hashCode()).isEqualTo(before.hashCode())
    }
}