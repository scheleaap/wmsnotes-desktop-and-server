package info.maaskant.wmsnotes.desktop.main.editing

import com.github.thomasnield.rxkotlinfx.actionEvents
import com.github.thomasnield.rxkotlinfx.observeOnFx
import info.maaskant.wmsnotes.desktop.main.ApplicationController
import info.maaskant.wmsnotes.desktop.main.ApplicationModel
import info.maaskant.wmsnotes.desktop.main.editing.editor.MarkdownEditorPane
import info.maaskant.wmsnotes.desktop.main.editing.preview.MarkdownPreviewPane
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.utilities.logger
import javafx.geometry.Orientation
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import tornadofx.*
import java.io.File

class DetailView : View() {

    private val logger by logger()

    private val applicationController: ApplicationController by inject()

    private val applicationModel: ApplicationModel by di()

    private val editingModel: EditingModel by di()

    private val commandProcessor: CommandProcessor by di()

    private val markdownEditorPane : MarkdownEditorPane by di()

    private val hboxesByAttachmentName: MutableMap<String, HBox> = mutableMapOf()

    override val root = splitpane {
        orientation = Orientation.HORIZONTAL
        setDividerPosition(0, 0.5)

        this += borderpane {
            center = markdownEditorPane.apply {
//                applicationModel.isSwitchingToNewlySelectedNote
//                        .observeOnFx()
//                        .subscribe(this::setDisable)
            }.node

            bottom = borderpane {
                center = vbox {
                    applicationModel.selectedNote
                            .observeOnFx()
                            .map { it.value?.attachments?.keys?.sorted() ?: emptyList() }
                            .subscribe { updateAttachments(it, this) }
                }

                right = button {
                    text = "Add attachment"
                    actionEvents()
                            .map { chooseImage() }
                            .filter { it.isNotEmpty() }
                            .map { it.first() }
                            .subscribe(applicationController.addAttachment)
                }

                applicationModel.isSwitchingToNewlySelectedNote
                        .observeOnFx()
                        .subscribe(this::setDisable) { logger.warn("Error", it) }
            }
        }

        this += MarkdownPreviewPane(editingModel).node
    }

    private fun updateAttachments(attachmentNames: List<String>, vbox: VBox) {
        val namesToDelete = HashSet(hboxesByAttachmentName.keys)
        for (name in attachmentNames) {
            if (name !in hboxesByAttachmentName) {
                val hbox = hbox {
                    this += label {
                        text = name
                    }
                    this += button {
                        text = "X"
                    }
                }
                vbox += hbox
                hboxesByAttachmentName[name] = hbox
            }
            namesToDelete.remove(name)
        }
        for (name in namesToDelete) {
            vbox.children.remove(hboxesByAttachmentName.remove(name))
        }
    }

    private fun chooseImage(): List<File> {
        return chooseFile(
                title = "Please choose a file to attach",
                filters = arrayOf(FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")),
                owner = this.currentWindow
        )
    }

    init {
//        applicationModel.allEventsWithUpdates
//                .observeOnFx()
//                .subscribe({
//                    when (it) {
//                        is NoteCreatedEvent -> noteCreated(it)
//                        is NoteDeletedEvent -> noteDeleted(it)
//                    }
//                }, { logger.warn("Error", it) })

    }

}
