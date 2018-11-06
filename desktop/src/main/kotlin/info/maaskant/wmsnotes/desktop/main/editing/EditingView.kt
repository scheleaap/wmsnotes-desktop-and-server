package info.maaskant.wmsnotes.desktop.main.editing

import com.github.thomasnield.rxkotlinfx.actionEvents
import com.github.thomasnield.rxkotlinfx.events
import com.github.thomasnield.rxkotlinfx.observeOnFx
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import info.maaskant.wmsnotes.desktop.design.Styles
import info.maaskant.wmsnotes.desktop.main.ApplicationController
import info.maaskant.wmsnotes.desktop.main.ApplicationModel
import info.maaskant.wmsnotes.desktop.main.editing.editor.MarkdownEditorPane
import info.maaskant.wmsnotes.desktop.main.editing.preview.MarkdownPreviewPane
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.utilities.logger
import javafx.event.EventType
import javafx.geometry.Orientation
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import tornadofx.*
import java.io.File

class EditingView : View() {

    private val logger by logger()

    private val applicationController: ApplicationController by inject()

    private val applicationModel: ApplicationModel by di()

    private val editingModel: EditingModel by di()

    private val commandProcessor: CommandProcessor by di()

    private val markdownEditorPane: MarkdownEditorPane by di()

    private val hboxesByAttachmentName: MutableMap<String, HBox> = mutableMapOf()

    override val root = splitpane {
        orientation = Orientation.HORIZONTAL
        setDividerPosition(0, 0.5)

        this += borderpane {
            center = markdownEditorPane.apply {
                //                TODO
//                applicationModel.isSwitchingToNewlySelectedNote
//                        .observeOnFx()
//                        .subscribe(this::setDisable)
            }.node

            bottom = borderpane {
                style {
                    padding = box(0.2.em, 0.2.em, 0.em, 0.2.em)
                }

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
                            .subscribe(applicationController.addAttachmentToCurrentNote)
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
                val hbox = HBox().apply {
                    label {
                        text = name
                        events(MouseEvent.MOUSE_CLICKED)
                                .subscribe {
                                    markdownEditorPane.smartEdit.insertImage(name, name)
                                    markdownEditorPane.requestFocus()
                                }
                    }
                    button {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.TIMES_CIRCLE).apply { size = "1.2em" }
                        addClass(Styles.borderlessButton)
                        action { applicationController.deleteAttachmentFromCurrentNote.onNext(name) }
                    }
                }
                vbox.children += hbox
                hboxesByAttachmentName[name] = hbox
            }
            namesToDelete.remove(name)
        }
        for (name in namesToDelete) {
            vbox.children -= hboxesByAttachmentName.remove(name)
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
