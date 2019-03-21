package info.maaskant.wmsnotes.desktop.client.indexing

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest

internal class KryoNoteIndexStateSerializerTest : KryoSerializerTest<NoteIndexState>() {
    private val aggId1 = "note-1"
    private val aggId2 = "note-2"

    override val items: List<NoteIndexState> = listOf(
            NoteIndexState(isInitialized = false, notes = emptyMap()),
            NoteIndexState(isInitialized = true, notes = mapOf(
                    (aggId1 to NoteMetadata(aggId = aggId1, title = "Title!", hidden = false))
            )),
            NoteIndexState(isInitialized = true, notes = mapOf(
                    (aggId1 to NoteMetadata(aggId = aggId1, title = "Title 1", hidden = false)),
                    (aggId2 to NoteMetadata(aggId = aggId2, title = "Title 2", hidden = true))
            ))
    )

    override fun createInstance(kryoPool: Pool<Kryo>) = KryoNoteIndexStateSerializer(kryoPool)
}
