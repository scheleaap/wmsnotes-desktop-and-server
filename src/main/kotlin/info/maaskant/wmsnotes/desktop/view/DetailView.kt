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
            text = "SDF"
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
                        .map {
                            chooseFile(
                                    title = "Please choose a file to attach",
                                    filters = arrayOf(FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"))
                                    // Make modal
                            )
                        }
                        .filter { it.isNotEmpty() }
                        .subscribe {
                            logger.info("NOW WOULD BE THE TIME TO ADD THE ATTACHMENT: $it")
                        }

            }
        }
    }

//    init {
//        applicationModel.allEventsWithUpdates
//                .observeOn(JavaFxScheduler.platform())
//                .subscribe({
//                    when (it) {
//                        is NoteCreatedEvent -> noteCreated(it)
//                        is NoteDeletedEvent -> noteDeleted(it)
//                    }
//                }, { logger.warn("Error", it) })
//    }

}
