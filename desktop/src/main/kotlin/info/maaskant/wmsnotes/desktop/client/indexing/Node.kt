package info.maaskant.wmsnotes.desktop.client.indexing

import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinToString
import info.maaskant.wmsnotes.model.Path
import java.util.*

sealed class Node(val aggId: String, val parentAggId: String?, val path: Path, val title: String) {
    override fun equals(other: Any?) =
            kotlinEquals(other = other, properties = arrayOf(Node::aggId, Node::parentAggId, Node::path, Node::title))

    override fun hashCode() = Objects.hash(aggId, title)
}

class Folder(aggId: String, parentAggId: String?, path: Path, title: String) : Node(aggId, parentAggId, path, title) {
    override fun toString() = kotlinToString(properties = arrayOf(Folder::aggId, Folder::parentAggId, Folder::path, Folder::title))
}

class Note(aggId: String, parentAggId: String?, path: Path, title: String) : Node(aggId, parentAggId, path, title) {
    override fun toString() = kotlinToString(properties = arrayOf(Note::aggId, Note::parentAggId, Note::path, Note::title))
}
