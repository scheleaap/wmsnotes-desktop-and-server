package info.maaskant.wmsnotes.desktop.view

import com.github.thomasnield.rxkotlinfx.observeOnFx
import info.maaskant.wmsnotes.desktop.app.Injector
import info.maaskant.wmsnotes.desktop.controller.ApplicationController
import info.maaskant.wmsnotes.desktop.model.ApplicationModel
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import info.maaskant.wmsnotes.utilities.Optional
import info.maaskant.wmsnotes.utilities.logger
import javafx.scene.control.TreeItem
import tornadofx.*

class TreeView : View() {

    private val logger by logger()

    private val applicationController: ApplicationController by inject()

    private val applicationModel: ApplicationModel = Injector.instance.applicationModel()

    private val commandProcessor: CommandProcessor = Injector.instance.commandProcessor()

    private val rootNode = TreeItem(NotebookNode(noteId = "root", title = "Root"))

    private val treeItemReferences = mutableMapOf<String, TreeItem<NotebookNode>>()

    override val root = treeview<NotebookNode> {
        root = rootNode
        root.isExpanded = true
        showRootProperty().set(false)
        cellFormat { text = it.title }
        onUserSelect {
            logger.debug("Selected: $it")
            applicationController.selectNote.onNext(Optional(it.noteId))
        }
    }

    init {
        applicationModel.allEventsWithUpdates
                .observeOnFx()
                .subscribe({
                    when (it) {
                        is NoteCreatedEvent -> noteCreated(it)
                        is NoteDeletedEvent -> noteDeleted(it)
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
