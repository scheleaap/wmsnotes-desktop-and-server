package info.maaskant.wmsnotes.desktop.main

import info.maaskant.wmsnotes.model.AddAttachmentCommand
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.DeleteAttachmentCommand
import info.maaskant.wmsnotes.model.DeleteNoteCommand
import info.maaskant.wmsnotes.utilities.Optional
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import tornadofx.*
import java.io.File

class ApplicationController : Controller() {

    private val logger by logger()

    private val applicationModel: ApplicationModel by di()

    private val commandProcessor: CommandProcessor by di()

    val selectNote: Subject<Optional<String>> = PublishSubject.create()
    val deleteCurrentNote: Subject<Unit> = PublishSubject.create()
    val addAttachmentToCurrentNote: Subject<File> = PublishSubject.create()
    val deleteAttachmentFromCurrentNote: Subject<String> = PublishSubject.create()

    init {
        selectNote.subscribe(applicationModel.selectedNoteId)
        deleteCurrentNote
                .filter { applicationModel.selectedNoteValue != null }
                .map { DeleteNoteCommand(applicationModel.selectedNoteValue!!.noteId, applicationModel.selectedNoteValue!!.revision) }
                .subscribe(commandProcessor.commands)
        addAttachmentToCurrentNote
                .filter { applicationModel.selectedNoteValue != null }
                .map {
                    AddAttachmentCommand(
                            noteId = applicationModel.selectedNoteValue!!.noteId,
                            lastRevision = applicationModel.selectedNoteValue!!.revision,
                            name = it.name,
                            content = it.readBytes()
                    )
                }
                .subscribe(commandProcessor.commands)
        deleteAttachmentFromCurrentNote
                .filter { applicationModel.selectedNoteValue != null }
                .map {
                    DeleteAttachmentCommand(
                            noteId = applicationModel.selectedNoteValue!!.noteId,
                            lastRevision = applicationModel.selectedNoteValue!!.revision,
                            name = it
                    )
                }
                .subscribe(commandProcessor.commands)
    }

}