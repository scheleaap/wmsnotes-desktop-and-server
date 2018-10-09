package info.maaskant.wmsnotes.desktop.view

import com.github.thomasnield.rxkotlinfx.actionEvents
import info.maaskant.wmsnotes.desktop.app.Injector
import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.desktop.controller.ApplicationController
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.CreateNoteCommand
import info.maaskant.wmsnotes.model.DeleteNoteCommand
import javafx.geometry.Orientation
import tornadofx.*

class ToolbarView : View() {

    private val logger by logger()

    private val applicationController: ApplicationController by inject()

    private val commandProcessor: CommandProcessor = Injector.instance.commandProcessor()

    override val root = toolbar {
        orientation = Orientation.HORIZONTAL
        button("Create") {
            tooltip("Create new note")
            actionEvents()
                    .subscribe {
                        //                        logger.info("$i")
                        commandProcessor.commands.onNext(CreateNoteCommand(null, "New Note"))
                    }


        }
        button("Delete") {
            tooltip("Delete the last note")
            actionEvents()
                    .subscribe {
                        applicationController.deleteCurrentNote.onNext(Unit)
                    }


        }

    }
}