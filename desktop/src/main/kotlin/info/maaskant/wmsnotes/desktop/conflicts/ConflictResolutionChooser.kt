package info.maaskant.wmsnotes.desktop.conflicts

import info.maaskant.wmsnotes.client.synchronization.Synchronizer
import info.maaskant.wmsnotes.desktop.util.Messages
import info.maaskant.wmsnotes.model.projection.NoteProjector
import javafx.scene.control.ButtonBar
import tornadofx.*
import java.util.*

class ConflictResolutionChooser : Fragment() {
    private val applicationTitle = "WMS Notes"

    val noteProjector: NoteProjector by di()
    val synchronizer:Synchronizer by di()

    val noteId: String by param()

    var result: Choice = Choice.NO_CHOICE
        private set

    override val root = borderpane {
        center = form {
            setPrefSize(940.0, 610.0)
            hbox(8) {
                fieldset(Messages["ConflictResolutionChooser.localVersion"]) {
                    this += find<NoteFragment>()
                }
                fieldset(Messages["ConflictResolutionChooser.remoteVersion"]) {
                    this += find<NoteFragment>()
                }
            }
        }
        bottom = buttonbar {
            button(Messages["dialog.cancel"], ButtonBar.ButtonData.CANCEL_CLOSE)
            button(Messages["dialog.ok"], ButtonBar.ButtonData.OK_DONE)
        }
    }

    init {
        title = applicationTitle

        println(noteId)
        println(synchronizer.getConflicts().blockingFirst())
    }

    enum class Choice {
        LOCAL,
        REMOTE,
        BOTH,
        NO_CHOICE,
    }
}

internal class NoteFragment : Fragment() {

    override val root = vbox {
        field(Messages["note.title"]) { textfield { isEditable = false;text = UUID.randomUUID().toString() } }
        field(Messages["note.content"]) {
            textarea {
                isEditable = false
                prefHeight = 200.0
            }
        }
        field(Messages["note.attachments"]) {
            vbox {
                label("att 1")
                label("att 2")
                label("att 3")
            }
        }
    }
}