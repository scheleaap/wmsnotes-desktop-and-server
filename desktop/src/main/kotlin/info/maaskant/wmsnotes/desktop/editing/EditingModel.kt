package info.maaskant.wmsnotes.desktop.editing

import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.parser.Parser
import info.maaskant.wmsnotes.desktop.editing.preview.Renderer
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.utilities.Optional
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.springframework.stereotype.Component
import javax.inject.Inject

@Component
class EditingModel @Inject constructor(
        private val renderer: Renderer
) {
    final val originalText: Subject<String> = PublishSubject.create()
    final val editedText: Subject<String> = PublishSubject.create()
    final val ast: Subject<Node> = PublishSubject.create()
    final val html: Subject<String> = PublishSubject.create()

    init {
        ast
                .map { renderer.render(it) }
                .subscribe(html)
    }

    fun nodeSelected(node: Optional<Note>) {
        if (node.value == null) {
//                                isDisable = true
            originalText.onNext("")
        } else {
            originalText.onNext("# " + node.value!!.title)
//                                isDisable = false
        }

    }

}