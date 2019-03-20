package info.maaskant.wmsnotes.desktop.main

import com.github.thomasnield.rxkotlinfx.events
import com.github.thomasnield.rxkotlinfx.observeOnFx
import info.maaskant.wmsnotes.desktop.client.indexing.Folder
import info.maaskant.wmsnotes.desktop.client.indexing.Note
import info.maaskant.wmsnotes.desktop.client.indexing.TreeIndex
import info.maaskant.wmsnotes.desktop.main.TreeView.NotebookNode.Type.FOLDER
import info.maaskant.wmsnotes.desktop.main.TreeView.NotebookNode.Type.NOTE
import info.maaskant.wmsnotes.model.CommandProcessor
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

    private val commandProcessor: CommandProcessor by di()

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
                treeIndex.getExistingNodesAsChanges(),
                treeIndex.getChanges()
        )
                .observeOnFx()
                .subscribe({
                    when (it) {
                        is TreeIndex.Change.NodeAdded -> {
                            when (it.metadata) {
                                is Folder -> addFolder(it.metadata)
                                is Note -> addNote(it.metadata)
                            }
                        }
                        is TreeIndex.Change.NodeRemoved -> removeNode(it.aggId)
                        is TreeIndex.Change.TitleChanged -> changeTitle(it.aggId, it.title)
                    }
                }, { logger.warn("Error", it) })

    }

    private fun addFolder(folder: Folder) {
        logger.debug("Adding folder ${folder.aggId}")
        val parentTreeItem: TreeItem<NotebookNode> = if (folder.parentAggId != null) {
            treeItemReferences[folder.parentAggId]!!
        } else {
            rootNode
        }
        val node = NotebookNode(aggId = folder.aggId, type = FOLDER, path = folder.path, title = folder.title)
        val treeItem = TreeItem(node)
        treeItemReferences[folder.aggId] = treeItem
        parentTreeItem += treeItem
    }

    private fun addNote(note: Note) {
        logger.debug("Adding note ${note.aggId}")
        val parentTreeItem: TreeItem<NotebookNode> = if (note.parentAggId != null) {
            treeItemReferences[note.parentAggId]!!
        } else {
            rootNode
        }
        val node = NotebookNode(aggId = note.aggId, type = NOTE, path = note.path, title = note.title)
        val treeItem = TreeItem(node)
        treeItemReferences[note.aggId] = treeItem
        parentTreeItem += treeItem
    }

    private fun changeTitle(aggId: String, title: String) {
        logger.debug("Changing title of note $aggId")
        val treeItem: TreeItem<NotebookNode> = treeItemReferences[aggId]!!
        treeItem.value = NotebookNode(aggId, type = NOTE, path = TODO(), title = title)
    }

    private fun removeNode(aggId: String) {
        logger.debug("Removing node $aggId")
        val treeItem = treeItemReferences.remove(aggId)
        rootNode.children.remove(treeItem)
    }

    data class NotebookNode(val aggId: String, val type: Type, val path: Path, val title: String) {
        enum class Type {
            FOLDER,
            NOTE
        }
    }

}
