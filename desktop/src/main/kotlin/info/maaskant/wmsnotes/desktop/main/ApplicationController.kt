package info.maaskant.wmsnotes.desktop.main

import info.maaskant.wmsnotes.desktop.main.editing.EditingViewModel
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.folder.CreateFolderCommand
import info.maaskant.wmsnotes.model.note.*
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDateTime
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
    // Folder
    final val createFolder: Subject<String> = PublishSubject.create()
    // Note
    final val selectNote: Subject<NavigationViewModel.SelectionRequest> = PublishSubject.create()
    final val createNote: Subject<Unit> = PublishSubject.create()
    final val deleteCurrentNote: Subject<Unit> = PublishSubject.create()
    final val renameCurrentNote: Subject<Unit> = PublishSubject.create()
    final val addAttachmentToCurrentNote: Subject<File> = PublishSubject.create()
    final val deleteAttachmentFromCurrentNote: Subject<String> = PublishSubject.create()
    final val saveContent: Subject<Unit> = PublishSubject.create()

    private var i: Int = 1

    init {
        // Folder
        createFolder
                .subscribeOn(Schedulers.computation())
                .map { CreateFolderCommand(path = navigationViewModel.currentPathValue.child(it), lastRevision = 0) }
                .subscribe(commandProcessor.commands)

        // Note
        selectNote.subscribe(navigationViewModel.selectionRequest)
        createNote
                .subscribeOn(Schedulers.computation())
                .map { CreateNoteCommand(aggId = null, path = navigationViewModel.currentPathValue, title = "Note ${i++}", content = "") }
                .subscribe(commandProcessor.commands)
        deleteCurrentNote
                .subscribeOn(Schedulers.computation())
                .filter { navigationViewModel.currentNoteValue != null }
                .map { DeleteNoteCommand(navigationViewModel.currentNoteValue!!.aggId, navigationViewModel.currentNoteValue!!.revision) }
                .subscribe(commandProcessor.commands)
        renameCurrentNote
                .subscribeOn(Schedulers.computation())
                .filter { navigationViewModel.currentNoteValue != null }
                .map { ChangeTitleCommand(navigationViewModel.currentNoteValue!!.aggId, navigationViewModel.currentNoteValue!!.revision, "random title " + LocalDateTime.now().toString()) }
                .subscribe(commandProcessor.commands)
        addAttachmentToCurrentNote
                .subscribeOn(Schedulers.computation())
                .filter { navigationViewModel.currentNoteValue != null }
                .map {
                    AddAttachmentCommand(
                            aggId = navigationViewModel.currentNoteValue!!.aggId,
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
                            aggId = navigationViewModel.currentNoteValue!!.aggId,
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
                            aggId = navigationViewModel.currentNoteValue!!.aggId,
                            lastRevision = navigationViewModel.currentNoteValue!!.revision,
                            content = editingViewModel.getText()
                    )
                }
                .doOnNext { logger.debug("Saving content of note ${it.aggId}") }
                .subscribe(commandProcessor.commands)
    }

}