package info.maaskant.wmsnotes.desktop.main

import com.github.thomasnield.rxkotlinfx.events
import com.github.thomasnield.rxkotlinfx.observeOnFx
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import info.maaskant.wmsnotes.model.note.NoteDeletedEvent
import info.maaskant.wmsnotes.model.note.NoteUndeletedEvent
import info.maaskant.wmsnotes.model.note.TitleChangedEvent
import info.maaskant.wmsnotes.utilities.logger
import javafx.scene.control.TreeItem
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import tornadofx.*

class TreeView : View() {

    private val logger by logger()

    private val applicationController: ApplicationController by di()

    private val navigationViewModel: NavigationViewModel by di()

    private val commandProcessor: CommandProcessor by di()

    private val rootNode = TreeItem(NotebookNode(aggId = "root", title = "Root"))

    private val treeItemReferences = mutableMapOf<String, TreeItem<NotebookNode>>()

    override val root = treeview<NotebookNode> {
        root = rootNode
        root.isExpanded = true
        showRootProperty().set(false)
        cellFormat { text = it.title }
        onUserSelect {
            logger.debug("Selected: $it")
            applicationController.selectNote.onNext(NavigationViewModel.Selection.NoteSelection(aggId = it.aggId, title = it.title))
        }
        events(KeyEvent.KEY_PRESSED)
                .filter { it.code == KeyCode.DELETE }
                .map { Unit }
                .subscribe(applicationController.deleteCurrentNote)
    }

    init {
        navigationViewModel.allEventsWithUpdates
                .observeOnFx()
                .subscribe({
                    when (it) {
                        is NoteCreatedEvent -> addNote(aggId = it.aggId, title = it.title)
                        is NoteDeletedEvent -> removeNote(it)
                        is NoteUndeletedEvent -> addNote(aggId = it.aggId, title = "TODO")
                        is TitleChangedEvent -> changeTitle(it)
                        else -> {
                        }
                    }
                }, { logger.warn("Error", it) })
    }

    private fun addNote(aggId: String, title: String) {
        logger.debug("Adding note $aggId")
        val node = NotebookNode(aggId = aggId, title = title)
        val treeItem = TreeItem(node)
        treeItemReferences[aggId] = treeItem
        rootNode += treeItem
    }

    private fun changeTitle(e: TitleChangedEvent) {
        logger.debug("Changing title of note ${e.aggId}")
        val treeItem: TreeItem<NotebookNode> = treeItemReferences[e.aggId]!!
        treeItem.value = NotebookNode(e.aggId, e.title)
    }

    private fun removeNote(e: NoteDeletedEvent) {
        logger.debug("Removing note ${e.aggId}")
        val treeItem = treeItemReferences.remove(e.aggId)
        rootNode.children.remove(treeItem)
    }

    data class NotebookNode(val aggId: String, val title: String)

}
