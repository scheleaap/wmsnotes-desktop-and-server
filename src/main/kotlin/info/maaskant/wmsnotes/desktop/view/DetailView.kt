package info.maaskant.wmsnotes.desktop.view

import com.github.thomasnield.rxkotlinfx.actionEvents
import info.maaskant.wmsnotes.desktop.app.Injector
import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.desktop.controller.ApplicationController
import info.maaskant.wmsnotes.desktop.model.ApplicationModel
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.CreateNoteCommand
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import info.maaskant.wmsnotes.utilities.Optional
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import javafx.scene.control.TreeItem
import javafx.stage.FileChooser
import tornadofx.*
import java.io.File

class DetailView : View() {

    private val logger by logger()

    private val applicationController: ApplicationController by inject()

    private val applicationModel: ApplicationModel = Injector.instance.applicationModel()

    private val commandProcessor: CommandProcessor = Injector.instance.commandProcessor()

    override val root = borderpane {

        center = textarea {
            applicationModel.selectedNoteUpdates
                    .observeOn(JavaFxScheduler.platform())
                    .subscribe {
                        if (it.value == null) {
                            isDisable = true
                            text = null
                        } else {
                            text = it.value.title
                            isDisable = false
                        }
                    }
        }

        bottom = borderpane {
            center = vbox {
                this += hbox {
                    this += label {
                        text = "HOI"
                    }
                    this += button {
                        text = "X"
                    }
                }
                this += hbox {
                    this += label {
                        text = "DOEI"
                    }
                    this += button {
                        text = "X"
                    }
                }
            }

            right = button {
                text = "Add attachment"
                actionEvents()
                        .map { chooseImage() }
                        .filter { it.isNotEmpty() }
                        .map { it.first() }
                        .subscribe(applicationController.addAttachment)
                applicationModel.selectedNoteIdUpdates
                        .map { !it.isPresent }
                        .subscribe(this::setDisable) { logger.warn("Error", it) }
            }
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
//                .observeOn(JavaFxScheduler.platform())
//                .subscribe({
//                    when (it) {
//                        is NoteCreatedEvent -> noteCreated(it)
//                        is NoteDeletedEvent -> noteDeleted(it)
//                    }
//                }, { logger.warn("Error", it) })

    }

}
