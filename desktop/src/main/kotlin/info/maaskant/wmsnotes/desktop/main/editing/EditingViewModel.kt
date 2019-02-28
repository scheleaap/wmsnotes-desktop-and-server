package info.maaskant.wmsnotes.desktop.main.editing

import com.vladsch.flexmark.ast.Node
import info.maaskant.wmsnotes.desktop.main.NavigationViewModel
import info.maaskant.wmsnotes.desktop.main.editing.preview.Renderer
import info.maaskant.wmsnotes.model.note.Note
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

    private final val isDirtySubject: Subject<Boolean> = BehaviorSubject.create()
    private var isDirtyValue: Boolean = false
    private final val isEnabledSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()
    private var isEnabledValue: Boolean = false
    private final val noteSubject: BehaviorSubject<Optional<Note>> = BehaviorSubject.create()
    private var noteValue: Note? = null
    private var textValue: String = ""
    private val textUpdatesForEditor: Subject<String> = PublishSubject.create()

    init {
        ast
                .subscribeOn(Schedulers.computation())
                .doOnNext { logger.debug("Rendering HTML") }
                .map { renderer.render(it) }
                .subscribe(html)

        setDirty(false)
        setEnabled(false)
        setNote(Optional())
        navigationViewModel.setNavigationAllowed(isDirty().map { !it })

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

    final fun isDirty(): Observable<Boolean> = isDirtySubject.distinctUntilChanged()

    @Synchronized
    private fun setDirty(dirty: Boolean) {
        this.isDirtyValue = dirty
        this.isDirtySubject.onNext(dirty)
    }

    final fun isEnabled(): Observable<Boolean> = isEnabledSubject.distinctUntilChanged()

    @Synchronized
    private fun setEnabled(enabled: Boolean) {
        this.isEnabledValue = enabled
        this.isEnabledSubject.onNext(enabled)
    }

    final fun getNote(): Observable<Optional<Note>> = noteSubject

    @Synchronized
    private fun setNote(note: Optional<Note>) {
        if (noteValue?.aggId == note.value?.aggId || !isDirtyValue) {
            if (!isDirtyValue) {
                setText(note, true)
            } else if (noteValue != null && noteValue?.aggId == note.value?.aggId && textValue == note.value?.content) {
                setDirty(false)
                setText(note, false)
            }
            this.noteValue = note.value
            this.noteSubject.onNext(note)
        } else {
            logger.warn("Attempt to change note while dirty (current=${this.noteValue}, new=$note)")
        }
    }

    @Synchronized
    fun getText() = textValue

    @Synchronized
    private fun setText(note: Optional<Note>, updateEditor: Boolean) {
        this.textValue = note.value?.content ?: ""
        if (updateEditor) {
            this.textUpdatesForEditor.onNext(this.textValue)
        }
    }

    @Synchronized
    fun setText(text: String) {
        val isSameAsNoteContent = text == noteValue?.content
        if (!isSameAsNoteContent && !isEnabledValue) {
            throw IllegalStateException("Cannot set text if editing is not allowed (\"$text\")")
        }
        this.textValue = text
        setDirty(!isSameAsNoteContent)
    }

    final fun getTextUpdatesForEditor(): Observable<String> = textUpdatesForEditor
}