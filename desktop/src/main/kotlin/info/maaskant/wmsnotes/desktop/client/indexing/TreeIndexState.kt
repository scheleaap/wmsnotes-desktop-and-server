package info.maaskant.wmsnotes.desktop.client.indexing

import info.maaskant.wmsnotes.model.Path

data class TreeIndexState(
        val folders: List<Path> = emptyList(),
        val notes: Map<String, Note> = emptyMap(),
        val hiddenNotes: List<String> = emptyList()
) {
    fun addNote(note: Note) = copy(notes = notes + (note.aggId to note))
    fun addFolder(path: Path) = copy(folders = folders + path)
    fun removeFolder(path: Path) = copy(folders = folders - path)
    fun hideNote(aggId: String) = copy(hiddenNotes = hiddenNotes + aggId)
    fun unhidNote(aggId: String) = copy(hiddenNotes = hiddenNotes - aggId)
}