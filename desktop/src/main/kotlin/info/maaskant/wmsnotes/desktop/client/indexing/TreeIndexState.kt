package info.maaskant.wmsnotes.desktop.client.indexing

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.Pool
import com.google.common.collect.ImmutableListMultimap
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.utilities.serialization.*
import javax.inject.Inject

data class TreeIndexState(
        val isInitialized: Boolean,
        val foldersWithChildren: ImmutableListMultimap<Path, String> = ImmutableListMultimap.of(),
        val notes: Map<String, Note> = emptyMap(),
        val autoFolders: Set<String> = emptySet()
) {
    fun initializationFinished(): TreeIndexState = copy(isInitialized = true)

    private fun addFolder(folder: Folder) = copy(
            foldersWithChildren = ImmutableListMultimap.builder<Path, String>()
                    .putAll(foldersWithChildren)
                    .put(folder.path, folder.aggId)
                    .build()
    )

    fun addAutoFolder(folder: Folder) =
            addFolder(folder).copy(autoFolders = autoFolders + folder.aggId)

    fun addNormalFolder(folder: Folder) =
            addFolder(folder)

    fun addNote(note: Note) = copy(
            foldersWithChildren = ImmutableListMultimap.builder<Path, String>()
                    .putAll(foldersWithChildren)
                    .put(note.path, note.aggId)
                    .build(),
            notes = notes + (note.aggId to note)
    )

    fun getNote(aggId: String) =
            notes.getValue(aggId)

    fun isAutoFolder(aggId: String) =
            aggId in autoFolders

    fun isNodeInFolder(aggId: String, path: Path) =
            aggId in foldersWithChildren.get(path)

    fun markFolderAsAuto(aggId: String) =
            copy(autoFolders = autoFolders + aggId)

    fun markFolderAsNormal(aggId: String) =
            copy(autoFolders = autoFolders - aggId)

    private fun removeFolder(aggId: String): TreeIndexState {
        val builder = ImmutableListMultimap.builder<Path, String>()
        foldersWithChildren.entries().asSequence()
                .filter { it.value != aggId }
                .forEach {
                    builder.put(it)
                }
        val foldersWithChildren = builder.build()
        return copy(foldersWithChildren = foldersWithChildren)
    }

    fun removeAutoFolder(aggId: String) =
            removeFolder(aggId).copy(autoFolders = autoFolders - aggId)

    fun removeNormalFolder(aggId: String) =
            removeFolder(aggId)

    fun removeNote(aggId: String): TreeIndexState {
        val builder = ImmutableListMultimap.builder<Path, String>()
        foldersWithChildren.entries().asSequence()
                .filter { it.value != aggId }
                .forEach { builder.put(it) }
        val foldersWithChildren = builder.build()
        return copy(foldersWithChildren = foldersWithChildren)
    }

    fun replaceNote(note: Note): TreeIndexState = copy(
            notes = notes + (note.aggId to note)
    )
}

class KryoTreeIndexStateSerializer @Inject constructor(kryoPool: Pool<Kryo>) : KryoSerializer<TreeIndexState>(
        kryoPool,
        Registration(TreeIndexState::class.java, KryoTreeIndexStateSerializer(), 81),
        Registration(Note::class.java, KryoNoteSerializer(), 82)
) {

    private class KryoTreeIndexStateSerializer : Serializer<TreeIndexState>() {
        override fun write(kryo: Kryo, output: Output, it: TreeIndexState) {
            output.writeBoolean(it.isInitialized)
            output.writeImmutableListMultimap(it.foldersWithChildren) { path, aggId ->
                output.writeString(path.toString())
                output.writeString(aggId)
            }
            output.writeMap(it.notes) { _, note ->
                kryo.writeObject(output, note)
            }
            output.writeSet(it.autoFolders) {aggId ->
                output.writeString(aggId)
            }
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out TreeIndexState>): TreeIndexState {
            val isInitialized = input.readBoolean()
            val foldersWithChildren = input.readImmutableListMultimap {
                val path = Path.from(input.readString())
                val aggId = input.readString()
                path to aggId
            }
            val notes = input.readMap {
                val note = kryo.readObject(input, Note::class.java) as Note
                note.aggId to note
            }
            val autoFolders = input.readSet {
                input.readString()
            }
            return TreeIndexState(
                    isInitialized = isInitialized,
                    foldersWithChildren = foldersWithChildren,
                    notes = notes,
                    autoFolders = autoFolders
            )
        }
    }

    private class KryoNoteSerializer : Serializer<Note>() {
        override fun write(kryo: Kryo, output: Output, it: Note) {
            output.writeString(it.aggId)
            output.writeString(it.path.toString())
            output.writeString(it.title)
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out Note>): Note {
            val aggId = input.readString()
            val path = Path.from(input.readString())
            val title = input.readString()
            return Note(aggId = aggId, path = path, title = title)
        }
    }
}