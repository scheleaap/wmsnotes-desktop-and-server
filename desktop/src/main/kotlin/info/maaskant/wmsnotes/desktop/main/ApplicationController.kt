package info.maaskant.wmsnotes.desktop.main

import info.maaskant.wmsnotes.desktop.imprt.MarkdownFilesImporter
import info.maaskant.wmsnotes.desktop.main.editing.EditingViewModel
import info.maaskant.wmsnotes.model.CommandBus
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.folder.CreateFolderCommand
import info.maaskant.wmsnotes.model.folder.FolderCommand
import info.maaskant.wmsnotes.model.folder.FolderCommandRequest
import info.maaskant.wmsnotes.model.note.*
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.springframework.stereotype.Component
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Component
class ApplicationController @Inject constructor(
        private val navigationViewModel: NavigationViewModel,
        private val editingViewModel: EditingViewModel,
        commandBus: CommandBus
) {
    private val logger by logger()

    // Folder
    final val createFolder: Subject<String> = PublishSubject.create<String>().toSerialized()

    // Note
    final val selectNote: Subject<NavigationViewModel.SelectionRequest> = PublishSubject.create<NavigationViewModel.SelectionRequest>().toSerialized()
    final val createNote: Subject<Unit> = PublishSubject.create<Unit>().toSerialized()
    final val deleteCurrentNote: Subject<Unit> = PublishSubject.create<Unit>().toSerialized()
    final val addAttachmentToCurrentNote: Subject<File> = PublishSubject.create<File>().toSerialized()
    final val deleteAttachmentFromCurrentNote: Subject<String> = PublishSubject.create<String>().toSerialized()
    final val saveContent: Subject<Unit> = PublishSubject.create<Unit>().toSerialized()
    final val importMarkdownFiles: Subject<File> = PublishSubject.create<File>().toSerialized()

    private var i: Int = 1

    init {
        // Folder
        createFolder
                .subscribeOn(Schedulers.computation())
                .map { CreateFolderCommand(path = navigationViewModel.currentPathValue.child(it)) }
                .map { FolderCommandRequest.of(it) }
                .subscribe(commandBus.requests)

        // Note
        selectNote.subscribe(navigationViewModel.selectionRequest)
        createNote
                .subscribeOn(Schedulers.computation())
                .map { CreateNoteCommand(aggId = Note.randomAggId(), path = navigationViewModel.currentPathValue, title = "Note ${i++}", content = "") }
                .map { NoteCommandRequest.of(it) }
                .subscribe(commandBus.requests)
        deleteCurrentNote
                .subscribeOn(Schedulers.computation())
                .filter { navigationViewModel.currentNoteValue != null }
                .map { DeleteNoteCommand(navigationViewModel.currentNoteValue!!.aggId) }
                .map { NoteCommandRequest.of(it) }
                .subscribe(commandBus.requests)
        addAttachmentToCurrentNote
                .subscribeOn(Schedulers.computation())
                .filter { navigationViewModel.currentNoteValue != null }
                .map {
                    AddAttachmentCommand(
                            aggId = navigationViewModel.currentNoteValue!!.aggId,
                            name = it.name,
                            content = it.readBytes()
                    )
                }
                .map { NoteCommandRequest.of(it) }
                .subscribe(commandBus.requests)
        deleteAttachmentFromCurrentNote
                .subscribeOn(Schedulers.computation())
                .filter { navigationViewModel.currentNoteValue != null }
                .map {
                    DeleteAttachmentCommand(
                            aggId = navigationViewModel.currentNoteValue!!.aggId,
                            name = it
                    )
                }
                .map { NoteCommandRequest.of(it) }
                .subscribe(commandBus.requests)
        saveContent
                .subscribeOn(Schedulers.computation())
                .filter { navigationViewModel.currentNoteValue != null }
                .map {
                    if (editingViewModel.isDirty().blockingFirst() == false) throw IllegalStateException()
                    ChangeContentCommand(
                            aggId = navigationViewModel.currentNoteValue!!.aggId,
                            content = editingViewModel.getText()
                    )
                }
                .map { NoteCommandRequest.of(it, lastRevision = navigationViewModel.currentNoteValue!!.revision) }
                .doOnNext { logger.debug("Saving content of note ${it.aggId}") }
                .subscribe(commandBus.requests)
        importMarkdownFiles
                .subscribeOn(Schedulers.io())
                .flatMap {
                    MarkdownFilesImporter.load(it, basePath = Path())
                }
                .map { MarkdownFilesImporter.mapToCommand(it) }
                .map {
                    when (it) {
                        is FolderCommand -> FolderCommandRequest.of(it)
                        is NoteCommand -> NoteCommandRequest.of(it)
                        else -> throw IllegalArgumentException(it.toString())
                    }
                }
                .subscribe(commandBus.requests)
    }

}