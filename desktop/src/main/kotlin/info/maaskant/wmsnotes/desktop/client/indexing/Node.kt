package info.maaskant.wmsnotes.desktop.client.indexing

import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinToString
import info.maaskant.wmsnotes.model.Path
import java.util.*

sealed class Node(val aggId: String, val path: Path, val title: String) {
    override fun equals(other: Any?) =
            kotlinEquals(other = other, properties = arrayOf(Node::aggId, Node::title))

    override fun hashCode() = Objects.hash(aggId, title)
}

class Folder(aggId: String, path: Path, title: String) : Node(aggId, path, title) {
    override fun toString() = kotlinToString(properties = arrayOf(Folder::aggId, Folder::title))
}

class Note(aggId: String, path: Path, title: String) : Node(aggId, path, title) {
    override fun toString() = kotlinToString(properties = arrayOf(Note::aggId, Note::title))
}
