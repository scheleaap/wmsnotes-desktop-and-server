package info.maaskant.wmsnotes.desktop.main

import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import tornadofx.*
import java.io.File

class ApplicationController : Controller() {

    private val logger by logger()

    private val applicationModel: ApplicationModel by di()

    private val commandProcessor: CommandProcessor by di()

    // TODO: Replace with SerializedSubject
    val selectNote: Subject<ApplicationModel.Selection> = PublishSubject.create()
    val createNote: Subject<Unit> = PublishSubject.create()
    val deleteCurrentNote: Subject<Unit> = PublishSubject.create()
    val addAttachmentToCurrentNote: Subject<File> = PublishSubject.create()
    val deleteAttachmentFromCurrentNote: Subject<String> = PublishSubject.create()

    private var i: Int = 1

    init {
        selectNote.subscribe(applicationModel.selectionRequest)
        createNote
                .map { CreateNoteCommand(null, "New Note ${i++}") }
                .subscribe(commandProcessor.commands)
        deleteCurrentNote
                .filter { applicationModel.currentNoteValue != null }
                .map { DeleteNoteCommand(applicationModel.currentNoteValue!!.noteId, applicationModel.currentNoteValue!!.revision) }
                .subscribe(commandProcessor.commands)
        addAttachmentToCurrentNote
                .filter { applicationModel.currentNoteValue != null }
                .map {
                    AddAttachmentCommand(
                            noteId = applicationModel.currentNoteValue!!.noteId,
                            lastRevision = applicationModel.currentNoteValue!!.revision,
                            name = it.name,
                            content = it.readBytes()
                    )
                }
                .subscribe(commandProcessor.commands)
        deleteAttachmentFromCurrentNote
                .filter { applicationModel.currentNoteValue != null }
                .map {
                    DeleteAttachmentCommand(
                            noteId = applicationModel.currentNoteValue!!.noteId,
                            lastRevision = applicationModel.currentNoteValue!!.revision,
                            name = it
                    )
                }
                .subscribe(commandProcessor.commands)
    }

}