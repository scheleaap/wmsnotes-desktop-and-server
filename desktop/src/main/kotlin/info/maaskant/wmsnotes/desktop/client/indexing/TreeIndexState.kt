package info.maaskant.wmsnotes.desktop.client.indexing

import com.google.common.collect.ImmutableListMultimap
import info.maaskant.wmsnotes.model.Path

data class TreeIndexState(
        val foldersWithChildren: ImmutableListMultimap<Path, String> = ImmutableListMultimap.of(),
        val notes: Map<String, Note> = emptyMap(),
        val autoFolders: Set<String> = emptySet()
) {
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
}
