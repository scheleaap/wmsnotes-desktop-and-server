package info.maaskant.wmsnotes.desktop.main

import com.github.thomasnield.rxkotlinfx.toObservable
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import info.maaskant.wmsnotes.client.synchronization.SynchronizationTask
import info.maaskant.wmsnotes.desktop.main.editing.editor.MarkdownEditorPane
import info.maaskant.wmsnotes.desktop.settings.ApplicationViewState
import info.maaskant.wmsnotes.desktop.util.*
import info.maaskant.wmsnotes.utilities.logger
import javafx.application.Platform
import javafx.geometry.Orientation
import javafx.scene.control.TextInputDialog
import org.controlsfx.control.ToggleSwitch
import tornadofx.*
import java.util.*

class MenuAndToolbarView : View() {

    private val largerIconSize = "1.2em"

    private val logger by logger()

    private val applicationController: ApplicationController by di()

    private val navigationViewModel: NavigationViewModel by di()

    private val applicationViewState: ApplicationViewState by di()

    private val synchronizationTask: SynchronizationTask by di()

    private val markdownEditorPane: MarkdownEditorPane by di()

    private val createFolderAction = StatelessAction(messageKey = "menu.file.createFolder", graphic = FontAwesomeIconView(FontAwesomeIcon.FOLDER_ALT).apply { size = largerIconSize },
            accelerator = "Shortcut+N") {
        val defaultFolderTitle = Messages["NewFolderDialog.defaultFolderTitle"]
        TextInputDialog(defaultFolderTitle).apply {
            title = Messages["NewFolderDialog.title"]
            contentText = Messages["NewFolderDialog.contentText"]
        }
                .showAndWait()
                .flatMap {
                    if (it.isNotBlank()) {
                        Optional.of(it)
                    } else {
                        Optional.empty()
                    }
                }
                .ifPresent { applicationController.createFolder.onNext(it) }
    }
    private val createNoteAction = StatelessAction(messageKey = "menu.file.createNote", graphic = FontAwesomeIconView(FontAwesomeIcon.FILE_TEXT_ALT).apply { size = largerIconSize },
            accelerator = "Shortcut+N") {
        applicationController.createNote.onNext(Unit)
    }
    private val deleteNoteAction = StatelessAction(messageKey = "menu.file.deleteNote", graphic = FontAwesomeIconView(FontAwesomeIcon.TRASH_ALT).apply { size = largerIconSize },
            enabled = navigationViewModel.currentSelection.map { it is NavigationViewModel.Selection.NoteSelection }) {
        applicationController.deleteCurrentNote.onNext(Unit)
    }
    private val renameNoteAction = StatelessAction(messageKey = "menu.file.renameNote",
            enabled = navigationViewModel.currentSelection.map { it is NavigationViewModel.Selection.NoteSelection }) {
        TextInputDialog("TODO").apply {
            title = Messages["RenameNoteDialog.title"]
            contentText = Messages["RenameNoteDialog.contentText"]
        }
                .showAndWait()
                .flatMap {
                    if (it.isNotBlank()) {
                        Optional.of(it)
                    } else {
                        Optional.empty()
                    }
                }
                .ifPresent { applicationController.renameCurrentNote.onNext(it) }
    }
    private val cutAction = StatelessAction(messageKey = "menu.edit.cut", graphic = FontAwesomeIconView(FontAwesomeIcon.CUT),
            accelerator = "Shortcut+X",
            enabled = navigationViewModel.currentSelection.map { it is NavigationViewModel.Selection.NoteSelection }) {
        markdownEditorPane.cut()
    }
    private val copyAction = StatelessAction(messageKey = "menu.edit.copy", graphic = FontAwesomeIconView(FontAwesomeIcon.COPY),
            accelerator = "Shortcut+C",
            enabled = navigationViewModel.currentSelection.map { it is NavigationViewModel.Selection.NoteSelection }) {
        markdownEditorPane.copy()
    }
    private val pasteAction = StatelessAction(messageKey = "menu.edit.paste", graphic = FontAwesomeIconView(FontAwesomeIcon.PASTE),
            accelerator = "Shortcut+V",
            enabled = navigationViewModel.currentSelection.map { it is NavigationViewModel.Selection.NoteSelection }) {
        markdownEditorPane.paste()
    }
    private val selectAllAction = StatelessAction(messageKey = "menu.edit.selectAll",
            accelerator = "Shortcut+A",
            enabled = navigationViewModel.currentSelection.map { it is NavigationViewModel.Selection.NoteSelection }) {
        markdownEditorPane.selectAll()
    }
    private val findAction = StatelessAction(messageKey = "menu.edit.find", graphic = FontAwesomeIconView(FontAwesomeIcon.SEARCH),
            accelerator = "Shortcut+F",
            enabled = navigationViewModel.currentSelection.map { it is NavigationViewModel.Selection.NoteSelection }) {
        markdownEditorPane.find(false)
    }
    private val replaceAction = StatelessAction(messageKey = "menu.edit.replace",
            accelerator = "Shortcut+H",
            enabled = navigationViewModel.currentSelection.map { it is NavigationViewModel.Selection.NoteSelection }) {
        markdownEditorPane.find(true)
    }
    private val findNextAction = StatelessAction(messageKey = "menu.edit.findNext",
            accelerator = "F3",
            enabled = navigationViewModel.currentSelection.map { it is NavigationViewModel.Selection.NoteSelection }) {
        markdownEditorPane.findNextPrevious(true)
    }
    private val findPreviousAction = StatelessAction(messageKey = "menu.edit.findPrevious",
            accelerator = "Shift+F3",
            enabled = navigationViewModel.currentSelection.map { it is NavigationViewModel.Selection.NoteSelection }) {
        markdownEditorPane.findNextPrevious(false)
    }
    private val insertBoldAction = StatelessAction(messageKey = "menu.insert.bold", graphic = FontAwesomeIconView(FontAwesomeIcon.BOLD),
            accelerator = "Shortcut+B",
            enabled = navigationViewModel.currentSelection.map { it is NavigationViewModel.Selection.NoteSelection }) {
        markdownEditorPane.smartEdit.insertBold(Messages["defaultText.bold"])
    }
    private val insertItalicAction = StatelessAction(messageKey = "menu.insert.italic", graphic = FontAwesomeIconView(FontAwesomeIcon.ITALIC),
            accelerator = "Shortcut+I",
            enabled = navigationViewModel.currentSelection.map { it is NavigationViewModel.Selection.NoteSelection }) {
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
                        item(createFolderAction)
                        separator()
                        item(exitAction)
                    }
                    menu(Messages["menu.edit"]) {
                        item(cutAction)
                        item(copyAction)
                        item(pasteAction)
                        item(selectAllAction)
                        separator()
                        item(findAction)
                        item(replaceAction)
                        item(findNextAction)
                        item(findPreviousAction)
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
                    button(renameNoteAction)
                    button(createFolderAction)
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
//                    button {
//                        text = "Resolve conflict"
//                        // TODO: WAIT UNTIL NOTE IS NOT DIRTY
//                        actionEvents()
//                                .map {
//                                    val chooser: ConflictResolutionChooser = find<ConflictResolutionChooser>(
//                                            scope = scope,
//                                            params = mapOf(ConflictResolutionChooser::aggId to "53507bde-be93-406f-9609-4d4406bddedb")
//                                    ).apply {
//                                        openModal(block = true)
//                                    }
//                                    chooser.choice.firstElement().blockingGet()
//                                }
//                                .subscribe {
//                                    println("CONFLICT RESOLVING RESULT: " + it)
//                                }
//                    }
                }
            }
}