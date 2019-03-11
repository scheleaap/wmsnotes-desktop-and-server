package info.maaskant.wmsnotes.desktop.main

import com.github.thomasnield.rxkotlinfx.events
import com.github.thomasnield.rxkotlinfx.observeOnFx
import info.maaskant.wmsnotes.desktop.client.indexing.Folder
import info.maaskant.wmsnotes.desktop.client.indexing.Note
import info.maaskant.wmsnotes.desktop.client.indexing.TreeIndex
import info.maaskant.wmsnotes.desktop.main.TreeView.NotebookNode.Type.FOLDER
import info.maaskant.wmsnotes.desktop.main.TreeView.NotebookNode.Type.NOTE
import info.maaskant.wmsnotes.model.CommandProcessor
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

    private val rootNode = TreeItem(NotebookNode(aggId = "root", type = FOLDER, title = "Root"))

    private val treeItemReferences = mutableMapOf<String, TreeItem<NotebookNode>>()

    override val root = treeview<NotebookNode> {
        root = rootNode
        root.isExpanded = true
        showRootProperty().set(false)
        cellFormat { text = it.title }
        onUserSelect {
            logger.debug("Selected: $it")
            when (it.type) {
                FOLDER -> applicationController.selectNote.onNext(NavigationViewModel.Selection.FolderSelection(aggId = it.aggId, title = it.title))
                NOTE -> applicationController.selectNote.onNext(NavigationViewModel.Selection.NoteSelection(aggId = it.aggId, title = it.title))
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
                                is Folder -> addFolder(aggId = it.metadata.aggId, title = it.metadata.path.toString())
                                is Note -> addNote(aggId = it.metadata.aggId, title = it.metadata.title)
                            }
                        }
                        is TreeIndex.Change.NodeRemoved -> removeNode(it.aggId)
                        is TreeIndex.Change.TitleChanged -> changeTitle(it.aggId, it.title)
                    }
                }, { logger.warn("Error", it) })

    }

    private fun addFolder(aggId: String, title: String) {
        logger.debug("Adding folder $aggId")
        val node = NotebookNode(aggId = aggId, type = FOLDER, title = title)
        val treeItem = TreeItem(node)
        treeItemReferences[aggId] = treeItem
        rootNode += treeItem
    }

    private fun addNote(aggId: String, title: String) {
        logger.debug("Adding note $aggId")
        val node = NotebookNode(aggId = aggId, type = NOTE, title = title)
        val treeItem = TreeItem(node)
        treeItemReferences[aggId] = treeItem
        rootNode += treeItem
    }

    private fun changeTitle(aggId: String, title: String) {
        logger.debug("Changing title of note $aggId")
        val treeItem: TreeItem<NotebookNode> = treeItemReferences[aggId]!!
        treeItem.value = NotebookNode(aggId, NOTE, title)
    }

    private fun removeNode(aggId: String) {
        logger.debug("Removing node $aggId")
        val treeItem = treeItemReferences.remove(aggId)
        rootNode.children.remove(treeItem)
    }

    data class NotebookNode(val aggId: String, val type: Type, val title: String) {
        enum class Type {
            FOLDER,
            NOTE
        }
    }

}
