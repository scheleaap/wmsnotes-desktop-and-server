package info.maaskant.wmsnotes.client.indexing

sealed class TreeIndexEvent {
    data class NodeAdded(val node: Node, val folderIndex: Int) : TreeIndexEvent()
    data class NodeRemoved(val node: Node) : TreeIndexEvent()
    data class TitleChanged(val node: Node, val oldFolderIndex: Int, val newFolderIndex: Int) : TreeIndexEvent()
}