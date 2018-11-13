package info.maaskant.wmsnotes.desktop.main

import com.github.thomasnield.rxkotlinfx.events
import com.github.thomasnield.rxkotlinfx.observeOnFx
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import info.maaskant.wmsnotes.utilities.logger
import javafx.scene.control.TreeItem
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import tornadofx.*

class TreeView : View() {

    private val logger by logger()

    private val applicationController: ApplicationController by inject()

    private val navigationViewModel: NavigationViewModel by di()

    private val commandProcessor: CommandProcessor by di()

    private val rootNode = TreeItem(NotebookNode(noteId = "root", title = "Root"))

    private val treeItemReferences = mutableMapOf<String, TreeItem<NotebookNode>>()

    override val root = treeview<NotebookNode> {
        root = rootNode
        root.isExpanded = true
        showRootProperty().set(false)
        cellFormat { text = it.title }
        onUserSelect {
            logger.debug("Selected: $it")
            applicationController.selectNote.onNext(NavigationViewModel.Selection.NoteSelection(noteId = it.noteId, title = it.title))
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
                        is NoteCreatedEvent -> noteCreated(it)
                        is NoteDeletedEvent -> noteDeleted(it)
                        else -> {
                        }
                    }
                }, { logger.warn("Error", it) })
    }

    private fun noteCreated(e: NoteCreatedEvent) {
        logger.debug("Adding note ${e.noteId}")
        val node = NotebookNode(noteId = e.noteId, title = e.title)
        val treeItem = TreeItem(node)
        treeItemReferences[e.noteId] = treeItem
        rootNode += treeItem
    }

    private fun noteDeleted(e: NoteDeletedEvent) {
        logger.debug("Removing note ${e.noteId}")
        val treeItem = treeItemReferences.remove(e.noteId)
        rootNode.children.remove(treeItem)
    }

    data class NotebookNode(val noteId: String, val title: String)

}
