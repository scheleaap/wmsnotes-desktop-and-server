package info.maaskant.wmsnotes.client.indexing

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializer
import info.maaskant.wmsnotes.utilities.serialization.readMap
import info.maaskant.wmsnotes.utilities.serialization.writeMapWithNullableValues
import javax.inject.Inject

data class NoteIndexState(val isInitialized: Boolean, val notes: Map<String, NoteMetadata> = emptyMap()) {
    fun initializationFinished(): NoteIndexState = copy(isInitialized = true)
    fun addNote(noteMetadata: NoteMetadata) = copy(notes = notes + (noteMetadata.noteId to noteMetadata))
    fun removeNote(noteId: String) = copy(notes = notes - noteId)
}

class KryoNoteIndexStateSerializer @Inject constructor(kryoPool: Pool<Kryo>) : KryoSerializer<NoteIndexState>(
        kryoPool,
        Registration(NoteIndexState::class.java, KryoNoteIndexStateSerializer(), 21),
        Registration(NoteMetadata::class.java, KryoNoteMetadataSerializer(), 22)
) {

    private class KryoNoteIndexStateSerializer : Serializer<NoteIndexState>() {
        override fun write(kryo: Kryo, output: Output, it: NoteIndexState) {
            output.writeBoolean(it.isInitialized)
            output.writeMapWithNullableValues(it.notes) { _, metadata ->
                kryo.writeObject(output, metadata)
            }
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out NoteIndexState>): NoteIndexState {
            val isInitialized = input.readBoolean()
            val notes = input.readMap {
                val metadata = kryo.readObject(input, NoteMetadata::class.java) as NoteMetadata
                metadata.noteId to metadata
            }
            return NoteIndexState(isInitialized = isInitialized, notes = notes)
        }
    }

    private class KryoNoteMetadataSerializer : Serializer<NoteMetadata>() {
        override fun write(kryo: Kryo, output: Output, it: NoteMetadata) {
            output.writeString(it.noteId)
            output.writeString(it.title)
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out NoteMetadata>): NoteMetadata {
            val noteId = input.readString()
            val title = input.readString()
            return NoteMetadata(noteId = noteId, title = title)
        }
    }
}