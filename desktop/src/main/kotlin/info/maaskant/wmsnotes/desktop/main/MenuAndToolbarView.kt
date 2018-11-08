package info.maaskant.wmsnotes.desktop.main

import com.github.thomasnield.rxkotlinfx.toObservable
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import info.maaskant.wmsnotes.client.synchronization.SynchronizationTask
import info.maaskant.wmsnotes.desktop.main.editing.editor.MarkdownEditorPane
import info.maaskant.wmsnotes.desktop.settings.ApplicationViewState
import info.maaskant.wmsnotes.desktop.util.*
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

    private val applicationViewState: ApplicationViewState by di()

    private val commandProcessor: CommandProcessor by di()

    private val synchronizationTask: SynchronizationTask by di()

    private val markdownEditorPane: MarkdownEditorPane by di()

    private var i: Int = 1

    private val createNoteAction = StatelessAction(messageKey = "menu.file.createNote", graphic = FontAwesomeIconView(FontAwesomeIcon.FILE_TEXT_ALT).apply { size = largerIconSize },
            accelerator = "Shortcut+N") {
        commandProcessor.commands.onNext(CreateNoteCommand(null, "New Note ${i++}"))
    }
    private val deleteNoteAction = StatelessAction(messageKey = "menu.file.deleteNote", graphic = FontAwesomeIconView(FontAwesomeIcon.TRASH_ALT).apply { size = largerIconSize },
            enabled = applicationModel.selectedNoteId.map { it.isPresent }) {
        applicationController.deleteCurrentNote.onNext(Unit)
    }
    private val insertBoldAction = StatelessAction(messageKey = "menu.insert.bold", graphic = FontAwesomeIconView(FontAwesomeIcon.BOLD),
            accelerator = "Shortcut+B",
            enabled = applicationModel.selectedNoteId.map { it.isPresent }) {
        markdownEditorPane.smartEdit.insertBold(Messages["defaultText.bold"])
    }
    private val insertItalicAction = StatelessAction(messageKey = "menu.insert.italic", graphic = FontAwesomeIconView(FontAwesomeIcon.ITALIC),
            accelerator = "Shortcut+I",
            enabled = applicationModel.selectedNoteId.map { it.isPresent }) {
        markdownEditorPane.smartEdit.insertItalic(Messages["defaultText.italic"])
    }
    private val toggleLineNumbersAction = StatefulAction(messageKey = "menu.view.showLineNumbers",
            accelerator = "Alt+L",
            active = applicationViewState.showLineNumbers) {
        applicationViewState.toggleShowLineNumbers()
    }
    private val toggleWhitespaceAction = StatefulAction(messageKey = "menu.view.showWhitespace", graphic = FontAwesomeIconView(FontAwesomeIcon.PARAGRAPH),
            accelerator = "Alt+W",
            active = applicationViewState.showWhitespace) {
        applicationViewState.toggleShowWhitespace()
    }

    private val exitAction = StatelessAction("menu.file.exit") { Platform.exit() }

    override val root =
            vbox {
                menubar {
                    menu(Messages["menu.file"]) {
                        item(createNoteAction)
                        item(deleteNoteAction)
                        separator()
                        item(exitAction)
                    }
                    menu(Messages["menu.insert"]) {
                        item(insertBoldAction)
                        item(insertItalicAction)
                    }
                    menu(Messages["menu.view"]) {
                        item(toggleLineNumbersAction)
                        item(toggleWhitespaceAction)
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