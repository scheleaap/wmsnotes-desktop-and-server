package info.maaskant.wmsnotes.desktop.main.editing.preview

import info.maaskant.wmsnotes.desktop.main.editing.EditingViewModel
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.springframework.stereotype.Component
import java.io.File
import javax.inject.Inject
import javax.inject.Qualifier

@Component
class PreviewAttachmentStorage @Inject constructor(
        @PreviewAttachmentDirectory private val rootDirectory: File,
        editingViewModel: EditingViewModel,
        initialState: PreviewAttachmentStorageState?,
        scheduler: Scheduler = Schedulers.computation()
) {

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class PreviewAttachmentDirectory

    private val logger by logger()

    private val attachmentsStoredNotifications: Subject<Map<String, String>> = BehaviorSubject.create()

    private var state: PreviewAttachmentStorageState = initialState ?: PreviewAttachmentStorageState()

    init {
        editingViewModel.getNote()
                .subscribeOn(scheduler)
                .map { it.value ?: Note() }
                .scan(Pair<Note?, Note>(null, Note())) { previousResult: Pair<Note?, Note>, new ->
                    previousResult.second to new
                }
                .subscribeBy(onNext = { (previous, new) ->
                    if (new.attachments.isNotEmpty()) {
                        rootDirectory.mkdirs()
                        new.attachments.forEach { (name, content) ->
                            val hash = new.attachmentHashes[name]!!
                            if (state.storedAttachments[name] != hash) {
                                rootDirectory.resolve(name).writeBytes(content)
                                updateState(state.addAttachment(name, hash))
                            }
                        }
                    }
                    if (previous?.attachmentHashes != new.attachmentHashes) {
                        attachmentsStoredNotifications.onNext(new.attachmentHashes)
                    }
                }, onError = { logger.warn("Error", it) })
    }

    fun getAttachmentsStoredNotifications(): Observable<Map<String, String>> = attachmentsStoredNotifications

    fun getPath(): File = rootDirectory

    private fun updateState(state: PreviewAttachmentStorageState) {
        this.state = state
    }

}