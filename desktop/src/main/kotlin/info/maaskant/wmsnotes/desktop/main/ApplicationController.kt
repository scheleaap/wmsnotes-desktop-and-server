package info.maaskant.wmsnotes.desktop.main

import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import tornadofx.*
import java.io.File

class ApplicationController : Controller() {

    private val logger by logger()

    private val navigationViewModel: NavigationViewModel by di()

    private val commandProcessor: CommandProcessor by di()

    // TODO: Replace with SerializedSubject
    val selectNote: Subject<NavigationViewModel.Selection> = PublishSubject.create()
    val createNote: Subject<Unit> = PublishSubject.create()
    val deleteCurrentNote: Subject<Unit> = PublishSubject.create()
    val addAttachmentToCurrentNote: Subject<File> = PublishSubject.create()
    val deleteAttachmentFromCurrentNote: Subject<String> = PublishSubject.create()

    private var i: Int = 1

    init {
        selectNote.subscribe(navigationViewModel.selectionRequest)
        createNote
                .map { CreateNoteCommand(null, "New Note ${i++}") }
                .subscribe(commandProcessor.commands)
        deleteCurrentNote
                .filter { navigationViewModel.currentNoteValue != null }
                .map { DeleteNoteCommand(navigationViewModel.currentNoteValue!!.noteId, navigationViewModel.currentNoteValue!!.revision) }
                .subscribe(commandProcessor.commands)
        addAttachmentToCurrentNote
                .filter { navigationViewModel.currentNoteValue != null }
                .map {
                    AddAttachmentCommand(
                            noteId = navigationViewModel.currentNoteValue!!.noteId,
                            lastRevision = navigationViewModel.currentNoteValue!!.revision,
                            name = it.name,
                            content = it.readBytes()
                    )
                }
                .subscribe(commandProcessor.commands)
        deleteAttachmentFromCurrentNote
                .filter { navigationViewModel.currentNoteValue != null }
                .map {
                    DeleteAttachmentCommand(
                            noteId = navigationViewModel.currentNoteValue!!.noteId,
                            lastRevision = navigationViewModel.currentNoteValue!!.revision,
                            name = it
                    )
                }
                .subscribe(commandProcessor.commands)
    }

}