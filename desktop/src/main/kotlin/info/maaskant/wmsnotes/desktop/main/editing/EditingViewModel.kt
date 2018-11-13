package info.maaskant.wmsnotes.desktop.main.editing

import com.vladsch.flexmark.ast.Node
import info.maaskant.wmsnotes.desktop.main.NavigationViewModel
import info.maaskant.wmsnotes.desktop.main.editing.preview.Renderer
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.utilities.Optional
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.springframework.stereotype.Component
import javax.inject.Inject

@Component
class EditingViewModel @Inject constructor(
        navigationViewModel: NavigationViewModel,
        private val renderer: Renderer,
        scheduler: Scheduler = Schedulers.computation()
) {
    private val logger by logger()

    // For rendering
    final val ast: Subject<Node> = PublishSubject.create()
    final val html: Subject<String> = PublishSubject.create()

    private final val isDirty: Subject<Boolean> = BehaviorSubject.create()
    private var isDirtyValue: Boolean = false
    private final val isEnabled: BehaviorSubject<Boolean> = BehaviorSubject.create()
    private var isEnabledValue: Boolean = false
    private final val note: BehaviorSubject<Optional<Note>> = BehaviorSubject.create()
    private var noteValue: Note? = null
    private var textValue: String = ""
    private val textUpdatesForEditor: Subject<String> = PublishSubject.create()

    init {
        ast
                .map { renderer.render(it) }
                .subscribe(html)

        setDirty(false)
        setEnabled(false)
        setNote(Optional())
        Observables.combineLatest(
                navigationViewModel.selectionSwitchingProcess
                        .subscribeOn(scheduler)
                        .filter { it !is NavigationViewModel.SelectionSwitchingProcessNotification.Loading }
                        .map { it is NavigationViewModel.SelectionSwitchingProcessNotification.Nothing }
                ,
                navigationViewModel.selectionSwitchingProcess
                        .subscribeOn(scheduler)
                        .filter { it is NavigationViewModel.SelectionSwitchingProcessNotification.Loading }
                        .map { (it as NavigationViewModel.SelectionSwitchingProcessNotification.Loading).loading }
        )
                .map { !(it.first || it.second) }
                .subscribe(::setEnabled) { logger.warn("Error", it) }

        navigationViewModel.selectionSwitchingProcess
                .subscribeOn(scheduler)
                .filter { it !is NavigationViewModel.SelectionSwitchingProcessNotification.Loading }
                .map {
                    when (it) {
                        is NavigationViewModel.SelectionSwitchingProcessNotification.Loading -> throw IllegalArgumentException()
                        NavigationViewModel.SelectionSwitchingProcessNotification.Nothing -> Optional<Note>()
                        is NavigationViewModel.SelectionSwitchingProcessNotification.Note -> Optional(it.note)
                    }
                }
                .subscribe(::setNote) { logger.warn("Error", it) }
    }

    fun isDirty(): Observable<Boolean> = isDirty.distinctUntilChanged()

    @Synchronized
    private fun setDirty(dirty: Boolean) {
        this.isDirtyValue = dirty
        this.isDirty.onNext(dirty)
    }

    fun isEnabled(): Observable<Boolean> = isEnabled.distinctUntilChanged()

    @Synchronized
    private fun setEnabled(enabled: Boolean) {
        this.isEnabledValue = enabled
        this.isEnabled.onNext(enabled)
    }

    fun getNote(): Observable<Optional<Note>> = note

    fun getTextUpdatesForEditor(): Observable<String> = textUpdatesForEditor

    @Synchronized
    private fun setNote(note: Optional<Note>) {
        if (noteValue?.noteId == note.value?.noteId || !isDirtyValue) {
            if (!isDirtyValue) {
                setTextInternal(note, true)
            } else if (noteValue != null && noteValue?.noteId == note.value?.noteId && textValue == note.value?.content) {
                setDirty(false)
                setTextInternal(note, true)
            }
            this.noteValue = note.value
            this.note.onNext(note)
        } else {
            logger.warn("Attempt to change note while dirty (current=${this.note.value}, new=$note)")
        }
    }

    @Synchronized
    private fun setTextInternal(note: Optional<Note>, updateEditor: Boolean) {
        this.textValue = note.value?.content ?: ""
        if (updateEditor) {
            this.textUpdatesForEditor.onNext(this.textValue)
        }
    }

    @Synchronized
    fun getText() = textValue

    @Synchronized
    fun setText(text: String) {
        val isSameAsNoteContent = text == noteValue?.content
        if (!isSameAsNoteContent && !isEnabledValue) {
            throw IllegalStateException("Cannot set text if editing is not allowed (\"$text\")")
        }
        this.textValue = text
        setDirty(!isSameAsNoteContent)
    }

}