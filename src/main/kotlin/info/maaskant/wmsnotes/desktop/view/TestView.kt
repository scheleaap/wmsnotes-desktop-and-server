package info.maaskant.wmsnotes.desktop.view

import com.github.thomasnield.rxkotlinfx.actionEvents
import info.maaskant.wmsnotes.desktop.app.Injector
import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.model.CreateNoteCommand
import info.maaskant.wmsnotes.model.DeleteNoteCommand
import info.maaskant.wmsnotes.model.Model
import javafx.geometry.Orientation
import tornadofx.*

class TestView : View() {

    private val logger by logger()

    private val model: Model = Injector.instance.model()

    private var i = 1

    override val root = toolbar {
        orientation = Orientation.HORIZONTAL
        button("Create") {
            tooltip("Create new note")
            actionEvents()
                    .subscribe {
                        //                        logger.info("$i")
                        model.commands.onNext(CreateNoteCommand(i.toString(), "Note $i"))
                        i++
                    }


        }
        button("Delete") {
            tooltip("Delete the last note")
            actionEvents()
                    .subscribe {
                        //                        logger.info("$i")
                        i--
                        model.commands.onNext(DeleteNoteCommand(i.toString()))
                    }


        }

    }
}