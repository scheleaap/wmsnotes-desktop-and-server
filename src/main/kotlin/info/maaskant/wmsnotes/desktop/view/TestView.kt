package info.maaskant.wmsnotes.desktop.view

import info.maaskant.wmsnotes.desktop.app.kodein
import info.maaskant.wmsnotes.desktop.app.logger
import com.github.thomasnield.rxkotlinfx.actionEvents
import com.github.thomasnield.rxkotlinfx.events
import info.maaskant.wmsnotes.model.CreateNoteCommand
import info.maaskant.wmsnotes.model.DeleteNoteCommand
import info.maaskant.wmsnotes.model.Model
import io.reactivex.Observable
import javafx.geometry.Orientation
import javafx.scene.control.TreeItem
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import info.maaskant.wmsnotes.model.*
import org.kodein.di.generic.instance
import tornadofx.*

class TestView : View() {

    private val logger by logger()

    private val model: Model by kodein.instance()


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