package info.maaskant.wmsnotes.desktop.view

import com.github.thomasnield.rxkotlinfx.actionEvents
import com.github.thomasnield.rxkotlinfx.observeOnFx
import info.maaskant.wmsnotes.desktop.controller.ApplicationController
import info.maaskant.wmsnotes.desktop.model.ApplicationModel
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.CreateNoteCommand
import info.maaskant.wmsnotes.utilities.logger
import javafx.geometry.Orientation
import tornadofx.*

class ToolbarView : View() {

    private val logger by logger()

    private val applicationController: ApplicationController by inject()

    private val applicationModel: ApplicationModel by di()

    private val commandProcessor: CommandProcessor by di()

    private var i: Int = 1

    override val root = toolbar {
        orientation = Orientation.HORIZONTAL
        button("Create") {
            tooltip("Create new note")
            actionEvents()
                    .map { CreateNoteCommand(null, "New Note ${i++}") }
                    .subscribe(commandProcessor.commands)
        }
        button("Delete") {
            tooltip("Delete the last note")
            actionEvents()
                    .map { Unit }
                    .subscribe(applicationController.deleteCurrentNote)
            applicationModel.selectedNoteId
                    .map { !it.isPresent }
                    .observeOnFx()
                    .subscribe(this::setDisable) { logger.warn("Error", it) }
        }
        progressindicator {
            progress = -1.0
            setPrefSize(16.0, 16.0)
        }
    }
}