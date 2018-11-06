package info.maaskant.wmsnotes.desktop.main

import com.github.thomasnield.rxkotlinfx.toObservable
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import info.maaskant.wmsnotes.client.synchronization.SynchronizationTask
import info.maaskant.wmsnotes.desktop.main.editing.editor.MarkdownEditorPane
import info.maaskant.wmsnotes.desktop.util.Action
import info.maaskant.wmsnotes.desktop.util.button
import info.maaskant.wmsnotes.desktop.util.item
import info.maaskant.wmsnotes.desktop.util.Messages
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.CreateNoteCommand
import info.maaskant.wmsnotes.utilities.logger
import javafx.application.Platform
import javafx.geometry.Orientation
import org.controlsfx.control.ToggleSwitch
import tornadofx.*

class MenuAndToolbarView : View() {

    private val largerIconSize = "1.2em"

    private val logger by logger()

    private val applicationController: ApplicationController by inject()

    private val applicationModel: ApplicationModel by di()

    private val commandProcessor: CommandProcessor by di()

    private val synchronizationTask: SynchronizationTask by di()

    private val markdownEditorPane: MarkdownEditorPane by di()

    private var i: Int = 1

    private val createNoteAction = Action(messageKey = "menu.file.create_note", graphic = FontAwesomeIconView(FontAwesomeIcon.FILE_TEXT_ALT).apply { size = largerIconSize },
            accelerator = "Shortcut+N") {
        commandProcessor.commands.onNext(CreateNoteCommand(null, "New Note ${i++}"))
    }
    private val deleteNoteAction = Action(messageKey = "menu.file.delete_note", graphic = FontAwesomeIconView(FontAwesomeIcon.TRASH_ALT).apply { size = largerIconSize },
            disable = applicationModel.selectedNoteId.map { !it.isPresent }) {
        applicationController.deleteCurrentNote.onNext(Unit)
    }

    private val exitAction = Action("menu.file.exit") { Platform.exit() }

    private val insertBoldAction = Action(messageKey = "menu.edit.bold", graphic = FontAwesomeIconView(FontAwesomeIcon.BOLD),
            accelerator = "Shortcut+B",
            disable = applicationModel.selectedNoteId.map { !it.isPresent }) {
        markdownEditorPane.smartEdit.insertBold(Messages["default_text.bold"])
    }
    private val insertItalicAction = Action(messageKey = "menu.edit.italic", graphic = FontAwesomeIconView(FontAwesomeIcon.ITALIC),
            accelerator = "Shortcut+I",
            disable = applicationModel.selectedNoteId.map { !it.isPresent }) {
        markdownEditorPane.smartEdit.insertItalic(Messages["default_text.italic"])
    }

    override val root =
            vbox {
                menubar {
                    menu(Messages["menu.file"]) {
                        item(createNoteAction)
                        item(deleteNoteAction)
                        separator()
                        item(exitAction)
                    }
                    menu(Messages["menu.edit"]) {
                        item(insertBoldAction)
                    }
                }
                toolbar {
                    orientation = Orientation.HORIZONTAL
                    button(createNoteAction)
                    button(deleteNoteAction)
                    // toggleswitch {
                    this += ToggleSwitch().apply {
                        selectedProperty().toObservable()
                                .subscribe {
                                    if (it) synchronizationTask.unpause() else synchronizationTask.pause()
                                }
                        synchronizationTask.isPaused()
                                .subscribe {
                                    this.isSelected = !it
                                }
                    }
                    progressindicator {
                        progress = -1.0
                        setPrefSize(16.0, 16.0)
                    }
                }
            }
}