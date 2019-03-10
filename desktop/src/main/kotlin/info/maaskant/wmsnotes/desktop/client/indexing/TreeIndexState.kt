package info.maaskant.wmsnotes.desktop.client.indexing

import com.google.common.collect.ImmutableListMultimap
import info.maaskant.wmsnotes.model.Path

data class TreeIndexState(
        val foldersWithChildren: ImmutableListMultimap<Path, String> = ImmutableListMultimap.of(),
        val notes: Map<String, Note> = emptyMap(),
        val hiddenNotes: Set<String> = emptySet()
) {
    fun addFolder(folder: Folder) = copy(
            foldersWithChildren = ImmutableListMultimap.builder<Path, String>()
                    .putAll(foldersWithChildren)
                    .put(folder.path, folder.aggId)
                    .build()
//            ,
//            notes = notes + (folder.aggId to folder)
    )

    fun addNote(note: Note) = copy(
            foldersWithChildren = ImmutableListMultimap.builder<Path, String>()
                    .putAll(foldersWithChildren)
                    .put(note.path, note.aggId)
                    .build(),
            notes = notes + (note.aggId to note)
    )

    fun removeFolder(aggId: String): TreeIndexState {
        val builder = ImmutableListMultimap.builder<Path, String>()
        foldersWithChildren.entries().asSequence()
                .filter { it.value != aggId }
                .forEach {
                    builder.put(it)
                }
        val foldersWithChildren = builder.build()
        return copy(
                foldersWithChildren = foldersWithChildren
        )
    }

    fun removeNote(aggId: String): TreeIndexState {
        val builder = ImmutableListMultimap.builder<Path, String>()
        foldersWithChildren.entries().asSequence()
                .filter { it.value != aggId }
                .forEach { builder.put(it) }
        val foldersWithChildren = builder.build()
        return copy(
                foldersWithChildren = foldersWithChildren
        )
    }

//    fun removeFolder(path: Path) = copy(folders = folders - path)
//    fun hideNote(aggId: String) = copy(hiddenNotes = hiddenNotes + aggId)
//    fun unhideNote(aggId: String) = copy(hiddenNotes = hiddenNotes - aggId)
}

//data class TreeIndexState(
//        val foldersWithChildren: ImmutableListMultimap<Path, Pair<String, Node>> = ImmutableListMultimap.of(),
//        //val folders: List<Path> = emptyList(),
//        //val notes: Map<String, Note> = emptyMap(),
//        val hiddenNotes: List<String> = emptyList()
//) {
//    fun addNote(note: Note) = copy(
//            //notes = notes + (note.aggId to note),
//            foldersWithChildren = ImmutableListMultimap.builder<Path, Pair<String, Node>>()
//                    .putAll(foldersWithChildren)
//                    .put(note.path, (note.aggId to note))
//                    .build()
//    )
//
//    fun addFolder(folder: Folder) = copy(
////            folders = folders + path
//            foldersWithChildren = ImmutableListMultimap.builder<Path, Pair<String, Node>>()
//                    .putAll(foldersWithChildren)
//                    .put(folder.path, (folder.aggId to folder))
//                    .build()
//    )
//
//    fun removeFolder(aggId: String): TreeIndexState {
//        val builder = ImmutableListMultimap.builder<Path, Pair<String, Node>>()
//        foldersWithChildren.entries().asSequence()
//                .filter { it.value.first != aggId }
//                .forEach {
//                    builder.put(it)
//                }
//        val foldersWithChildren = builder.build()
//        return copy(
////            folders = folders - path
//                foldersWithChildren = foldersWithChildren
//        )
//    }
//
//    fun hideNote(aggId: String) = copy(hiddenNotes = hiddenNotes + aggId)
//    fun unhideNote(aggId: String) = copy(hiddenNotes = hiddenNotes - aggId)
//}