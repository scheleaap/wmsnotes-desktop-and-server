package info.maaskant.wmsnotes.client.indexing

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest

internal class KryoNoteIndexStateSerializerTest : KryoSerializerTest<NoteIndexState>() {
    private val noteId = "note"

    override val items: List<NoteIndexState> = listOf(
            NoteIndexState(isInitialized = false, notes = emptyMap()),
            NoteIndexState(isInitialized = true, notes = mapOf(
                    (noteId to NoteMetadata(noteId = noteId, title = "Title!"))
            )),
            NoteIndexState(isInitialized = true, notes = mapOf(
                    (noteId to NoteMetadata(noteId = noteId, title = "Title!")),
                    (noteId to NoteMetadata(noteId = noteId, title = "Title!"))
            ))
    )

    override fun createInstance(kryoPool: Pool<Kryo>) = KryoNoteIndexStateSerializer(kryoPool)
}
