package info.maaskant.wmsnotes.desktop.main

import info.maaskant.wmsnotes.desktop.main.editing.EditingViewModel
import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.springframework.stereotype.Component
import tornadofx.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Component
class ApplicationController @Inject constructor(
        private val navigationViewModel: NavigationViewModel,
        private val editingViewModel: EditingViewModel,
        commandProcessor: CommandProcessor
) {

    private val logger by logger()

    // TODO: Replace with SerializedSubject
    final val selectNote: Subject<NavigationViewModel.Selection> = PublishSubject.create()
    final val createNote: Subject<Unit> = PublishSubject.create()
    final val deleteCurrentNote: Subject<Unit> = PublishSubject.create()
    final val addAttachmentToCurrentNote: Subject<File> = PublishSubject.create()
    final val deleteAttachmentFromCurrentNote: Subject<String> = PublishSubject.create()
    final val saveContent: Subject<Unit> = PublishSubject.create()

    private var i: Int = 1

    init {
        selectNote.subscribe(navigationViewModel.selectionRequest)
        createNote
                .subscribeOn(Schedulers.computation())
                .map { CreateNoteCommand(null, path = Path(), title = "New Note ${i++}", content = "") }
                .subscribe(commandProcessor.commands)
        deleteCurrentNote
                .subscribeOn(Schedulers.computation())
                .filter { navigationViewModel.currentNoteValue != null }
                .map { DeleteNoteCommand(navigationViewModel.currentNoteValue!!.noteId, navigationViewModel.currentNoteValue!!.revision) }
                .subscribe(commandProcessor.commands)
        addAttachmentToCurrentNote
                .subscribeOn(Schedulers.computation())
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
                .subscribeOn(Schedulers.computation())
                .filter { navigationViewModel.currentNoteValue != null }
                .map {
                    DeleteAttachmentCommand(
                            noteId = navigationViewModel.currentNoteValue!!.noteId,
                            lastRevision = navigationViewModel.currentNoteValue!!.revision,
                            name = it
                    )
                }
                .subscribe(commandProcessor.commands)
        saveContent
                .subscribeOn(Schedulers.computation())
                .filter { navigationViewModel.currentNoteValue != null }
                .map {
                    if (editingViewModel.isDirty().blockingFirst() == false) throw IllegalStateException()
                    ChangeContentCommand(
                            noteId = navigationViewModel.currentNoteValue!!.noteId,
                            lastRevision = navigationViewModel.currentNoteValue!!.revision,
                            content = editingViewModel.getText()
                    )
                }
                .doOnNext { logger.debug("Saving content of note ${it.noteId}") }
                .subscribe(commandProcessor.commands)
    }

}