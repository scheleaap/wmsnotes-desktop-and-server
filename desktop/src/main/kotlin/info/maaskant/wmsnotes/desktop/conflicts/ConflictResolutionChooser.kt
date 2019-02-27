package info.maaskant.wmsnotes.desktop.conflicts

import com.github.thomasnield.rxkotlinfx.actionEvents
import info.maaskant.wmsnotes.desktop.util.Messages
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.NoteProjector
import io.reactivex.subjects.BehaviorSubject
import javafx.geometry.Orientation
import javafx.scene.control.ButtonBar
import javafx.scene.layout.Priority
import tornadofx.*


class ConflictResolutionChooser : Fragment() {
    private val padding = 0.75.em

//    private val synchronizer: Synchronizer by di()

    val noteId: String by param()
    private val windowTitle = "Conflict Resolution of note $noteId"
//    private val conflictData = synchronizer.getConflictData(noteId)
//    private val localNote = NoteProjector.project(conflictData.base, conflictData.localConflictingEvents)
//    private val remoteNote = NoteProjector.project(conflictData.base, conflictData.remoteConflictingEvents)
//
//    val choice: BehaviorSubject<Choice> = BehaviorSubject.createDefault(Choice.NO_CHOICE)

    override val root = borderpane {
//        style { padding = box(this@ConflictResolutionChooser.padding) }
//        center = form {
//            style { padding = box(0.em) }
//            vbox {
//                hbox {
//                    style { spacing = this@ConflictResolutionChooser.padding }
//                    this += find<NoteFragment>(
//                            NoteFragment::fieldsetTitle to Messages["ConflictResolutionChooser.localVersion"],
//                            NoteFragment::note to localNote
//                    )
//                    this += find<NoteFragment>(
//                            NoteFragment::fieldsetTitle to Messages["ConflictResolutionChooser.remoteVersion"],
//                            NoteFragment::note to remoteNote
//                    )
//                }
//                fieldset("Choice") {
//                    hbox {
//                        style { padding = box(top = this@ConflictResolutionChooser.padding, right = 0.em, bottom = 0.em, left = 0.em) }
//                        val group = togglegroup()
//                        radiobutton(Messages["ConflictResolutionChooser.keepLocalVersion"], group = group) {
//                            actionEvents().map { Choice.LOCAL }.subscribe(choice)
//                        }
//                        region { hgrow = Priority.ALWAYS }
//                        radiobutton(Messages["ConflictResolutionChooser.keepBoth"], group = group){
//                            actionEvents().map { Choice.BOTH }.subscribe(choice)
//                        }
//                        region { hgrow = Priority.ALWAYS }
//                        radiobutton(Messages["ConflictResolutionChooser.keepRemoteVersion"], group = group){
//                            actionEvents().map { Choice.REMOTE }.subscribe(choice)
//                        }
//                    }
//                }
//            }
//        }
//        bottom = buttonbar {
//            style { padding = box(top = this@ConflictResolutionChooser.padding, right = 0.em, bottom = 0.em, left = 0.em) }
//            button(Messages["dialog.cancel"], ButtonBar.ButtonData.CANCEL_CLOSE) {
//                actionEvents().subscribe { close() }
//            }
//            button(Messages["dialog.ok"], ButtonBar.ButtonData.OK_DONE) {
//                choice.map { it == Choice.NO_CHOICE }.subscribe(this::setDisable)
//                actionEvents().subscribe { close() }
//            }
//        }
    }

    init {
        title = windowTitle

    }

    enum class Choice {
        LOCAL,
        REMOTE,
        BOTH,
        NO_CHOICE,
    }
}

internal class NoteFragment : Fragment() {
    val fieldsetTitle: String by param()
    val note: Note by param()

    override val root = fieldset(fieldsetTitle, labelPosition = Orientation.VERTICAL) {
        vbox {
            field(Messages["note.title"]) {
                textfield {
                    isEditable = false
                    text = note.title
                }
            }
            field(Messages["note.content"], Orientation.VERTICAL) {
                textarea {
                    isEditable = false
                    prefHeight = 200.0
                    text = note.content
                    vgrow = Priority.ALWAYS
                }
            }
            field(Messages["note.attachments"]) {
                vbox {
                    for ((name, hash) in note.attachmentHashes) {
                        label("$name ($hash)")
                    }
                }
            }
        }
    }
}