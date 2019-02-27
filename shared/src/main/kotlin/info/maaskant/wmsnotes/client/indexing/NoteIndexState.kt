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

data class NoteMetadata(val noteId: String, val title: String, val hidden: Boolean = false)

data class NoteIndexState(val isInitialized: Boolean, val notes: Map<String, NoteMetadata> = emptyMap()) {
    fun initializationFinished(): NoteIndexState = copy(isInitialized = true)
    fun addNote(noteId: String, title: String) = copy(notes = notes + (noteId to NoteMetadata(noteId = noteId, title = title, hidden = false)))
    fun hideNote(noteId: String) = copy(notes = notes + (noteId to notes.getValue(noteId).copy(hidden = true)))
    fun showNote(noteId: String) = copy(notes = notes + (noteId to notes.getValue(noteId).copy(hidden = false)))
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
            output.writeBoolean(it.hidden)
            output.writeString(it.title)
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out NoteMetadata>): NoteMetadata {
            val noteId = input.readString()
            val hidden = input.readBoolean()
            val title = input.readString()
            return NoteMetadata(noteId = noteId, title = title, hidden = hidden)
        }
    }
}