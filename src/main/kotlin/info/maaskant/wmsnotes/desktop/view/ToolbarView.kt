package info.maaskant.wmsnotes.desktop.view

import com.github.thomasnield.rxkotlinfx.actionEvents
import info.maaskant.wmsnotes.desktop.app.Injector
import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.desktop.controller.ApplicationController
import info.maaskant.wmsnotes.desktop.model.ApplicationModel
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.CreateNoteCommand
import io.reactivex.rxjavafx.observables.JavaFxObservable
import javafx.geometry.Orientation
import tornadofx.View
import tornadofx.button
import tornadofx.toolbar
import tornadofx.tooltip

class ToolbarView : View() {

    private val logger by logger()

    private val applicationController: ApplicationController by inject()

    private val applicationModel: ApplicationModel = Injector.instance.applicationModel()

    private val commandProcessor: CommandProcessor = Injector.instance.commandProcessor()

    override val root = toolbar {
        orientation = Orientation.HORIZONTAL
        button("Create") {
            tooltip("Create new note")
            actionEvents()
                    .subscribe {
                        commandProcessor.commands.onNext(CreateNoteCommand(null, "New Note"))
                    }
        }
        button("Delete") {
            tooltip("Delete the last note")
            actionEvents()
                    .map { Unit }
                    .subscribe(applicationController.deleteCurrentNote)
            applicationModel.selectedNoteIdUpdates
                    .map { !it.isPresent }
                    .subscribe(this::setDisable) { logger.warn("Error", it) }
        }

    }
}