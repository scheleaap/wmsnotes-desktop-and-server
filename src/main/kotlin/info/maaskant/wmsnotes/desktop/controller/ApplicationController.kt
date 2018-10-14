package info.maaskant.wmsnotes.desktop.controller

import info.maaskant.wmsnotes.desktop.app.Injector
import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.desktop.model.ApplicationModel
import info.maaskant.wmsnotes.model.AddAttachmentCommand
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.DeleteNoteCommand
import info.maaskant.wmsnotes.utilities.Optional
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import tornadofx.*
import java.io.File

class ApplicationController : Controller() {

    private val logger by logger()

    private val applicationModel: ApplicationModel = Injector.instance.applicationModel()

    private val commandProcessor: CommandProcessor = Injector.instance.commandProcessor()

    val selectNote: Subject<Optional<String>> = PublishSubject.create()
    val deleteCurrentNote: Subject<Unit> = PublishSubject.create()
    val addAttachment: Subject<File> = PublishSubject.create()

    init {
        selectNote.subscribe(applicationModel.selectedNoteIdUpdates)
        deleteCurrentNote
                .filter { applicationModel.selectedNote != null }
                .map { DeleteNoteCommand(applicationModel.selectedNote!!.noteId, applicationModel.selectedNote!!.revision) }
                .subscribe(commandProcessor.commands)
        addAttachment
                .filter { applicationModel.selectedNote != null }
                .map {
                    AddAttachmentCommand(
                            noteId = applicationModel.selectedNote!!.noteId,
                            lastRevision = applicationModel.selectedNote!!.revision,
                            name = it.name,
                            content = it.readBytes()
                    )
                }
                .subscribe(commandProcessor.commands)
    }

}