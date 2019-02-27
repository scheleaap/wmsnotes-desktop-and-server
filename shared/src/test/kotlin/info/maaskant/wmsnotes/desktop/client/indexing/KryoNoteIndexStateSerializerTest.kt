package info.maaskant.wmsnotes.desktop.client.indexing

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest

internal class KryoNoteIndexStateSerializerTest : KryoSerializerTest<NoteIndexState>() {
    private val noteId1 = "note-1"
    private val noteId2 = "note-2"

    override val items: List<NoteIndexState> = listOf(
            NoteIndexState(isInitialized = false, notes = emptyMap()),
            NoteIndexState(isInitialized = true, notes = mapOf(
                    (noteId1 to NoteMetadata(noteId = noteId1, title = "Title!", hidden = false))
            )),
            NoteIndexState(isInitialized = true, notes = mapOf(
                    (noteId1 to NoteMetadata(noteId = noteId1, title = "Title 1", hidden = false)),
                    (noteId2 to NoteMetadata(noteId = noteId2, title = "Title 2", hidden = true))
            ))
    )

    override fun createInstance(kryoPool: Pool<Kryo>) = KryoNoteIndexStateSerializer(kryoPool)
}
