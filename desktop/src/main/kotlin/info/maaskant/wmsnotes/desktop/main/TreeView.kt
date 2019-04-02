package info.maaskant.wmsnotes.desktop.main

import com.github.thomasnield.rxkotlinfx.events
import com.github.thomasnield.rxkotlinfx.observeOnFx
import info.maaskant.wmsnotes.client.indexing.Folder
import info.maaskant.wmsnotes.client.indexing.Note
import info.maaskant.wmsnotes.client.indexing.TreeIndex
import info.maaskant.wmsnotes.client.indexing.TreeIndex.Companion.asNodeAddedEvents
import info.maaskant.wmsnotes.client.indexing.TreeIndexEvent
import info.maaskant.wmsnotes.desktop.main.TreeView.NotebookNode.Type.FOLDER
import info.maaskant.wmsnotes.desktop.main.TreeView.NotebookNode.Type.NOTE
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import javafx.scene.control.TreeItem
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import tornadofx.*

class TreeView : View() {

    private val logger by logger()

    private val applicationController: ApplicationController by di()

    private val treeIndex: TreeIndex by di()

    private val rootNode = TreeItem(NotebookNode(aggId = "root", type = FOLDER, path = Path(), title = "Root"))

    private val treeItemReferences = mutableMapOf<String, TreeItem<NotebookNode>>()

    override val root = treeview<NotebookNode> {
        root = rootNode
        root.isExpanded = true
        showRootProperty().set(false)
        cellFormat { text = it.title }
        onUserSelect {
            logger.debug("Selected: $it")
            when (it.type) {
                FOLDER -> applicationController.selectNote.onNext(NavigationViewModel.SelectionRequest.FolderSelectionRequest(aggId = it.aggId, path = it.path, title = it.title))
                NOTE -> applicationController.selectNote.onNext(NavigationViewModel.SelectionRequest.NoteSelectionRequest(aggId = it.aggId))
            }
        }
        events(KeyEvent.KEY_PRESSED)
                .filter { it.code == KeyCode.DELETE }
                .map { Unit }
                .subscribe(applicationController.deleteCurrentNote)
    }

    init {
        Observable.concat(
                treeIndex.getNodes().compose(asNodeAddedEvents()),
                treeIndex.getEvents()
        )
                .observeOnFx()
                .subscribe({
                    when (it) {
                        is TreeIndexEvent.NodeAdded -> {
                            when (it.node) {
                                is Folder -> addFolder(it.node as Folder, it.folderIndex)
                                is Note -> addNote(it.node as Note, it.folderIndex)
                            }
                        }
                        is TreeIndexEvent.NodeRemoved -> removeNode(it.node.aggId)
                        is TreeIndexEvent.TitleChanged -> changeTitle(it.node.aggId, it.node.title, it.oldFolderIndex, it.newFolderIndex)
                    }
                }, { logger.warn("Error", it) })

    }

    private fun addFolder(folder: Folder, folderIndex: Int) {
        logger.debug("Adding folder ${folder.aggId}")
        val parentTreeItem: TreeItem<NotebookNode> = if (folder.parentAggId != null) {
            treeItemReferences[folder.parentAggId!!]!!
        } else {
            rootNode
        }
        val node = NotebookNode(aggId = folder.aggId, type = FOLDER, path = folder.path, title = folder.title)
        val treeItem = TreeItem(node)
        treeItemReferences[folder.aggId] = treeItem
        parentTreeItem.children.add(folderIndex, treeItem)
    }

    private fun addNote(note: Note, folderIndex: Int) {
        logger.debug("Adding note ${note.aggId}")
        val parentTreeItem: TreeItem<NotebookNode> = if (note.parentAggId != null) {
            treeItemReferences[note.parentAggId!!]!!
        } else {
            rootNode
        }
        val node = NotebookNode(aggId = note.aggId, type = NOTE, path = note.path, title = note.title)
        val treeItem = TreeItem(node)
        treeItemReferences[note.aggId] = treeItem
        parentTreeItem.children.add(folderIndex, treeItem)
    }

    private fun changeTitle(aggId: String, title: String, oldFolderIndex: Int, newFolderIndex: Int) {
        logger.debug("Changing title of note $aggId")
        val treeItem: TreeItem<NotebookNode> = treeItemReferences[aggId]!!
        val oldNode = treeItem.value
        val newNode = NotebookNode(oldNode.aggId, type = oldNode.type, path = oldNode.path, title = title)
        treeItem.value = newNode
        if (oldFolderIndex != newFolderIndex) {
            // Disabled reselection, as this can inadvertently change a note's content (if a folder is selected briefly)
//            val reselect = root.selectionModel.selectedItem == treeItem
            val parentTreeItem = treeItem.parent
            parentTreeItem.children.removeAt(oldFolderIndex)
            parentTreeItem.children.add(newFolderIndex, treeItem)
//            if (reselect) {
//                root.selectionModel.select(treeItem)
//            }
        }
    }

    private fun removeNode(aggId: String) {
        logger.debug("Removing node $aggId")
        val treeItem = treeItemReferences.remove(aggId)
        rootNode.children.remove(treeItem)
        TODO("This is not implemented correctly")
    }

    data class NotebookNode(val aggId: String, val type: Type, val path: Path, val title: String) {
        enum class Type {
            FOLDER,
            NOTE
        }
    }

}
