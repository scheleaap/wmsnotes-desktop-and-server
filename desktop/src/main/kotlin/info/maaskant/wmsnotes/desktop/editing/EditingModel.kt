package info.maaskant.wmsnotes.desktop.editing

import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.parser.Parser
import info.maaskant.wmsnotes.desktop.preview.Renderer
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.springframework.stereotype.Component
import java.io.File
import javax.inject.Inject

@Component
class EditingModel @Inject constructor(
        private val renderer: Renderer
) {
    final val markdownText: Subject<String> = PublishSubject.create()
    final val markdownAst: Subject<Node> = PublishSubject.create()
    final val html: Subject<String> = PublishSubject.create()

    private val parser = Parser.builder()
            //.extensions(MarkdownExtensions.getFlexmarkExtensions(Options.getMarkdownRenderer()))
            .build()

    init {
        markdownText
                .map { parseMarkdown(it) }
                .subscribe(markdownAst)
        markdownAst
                .map { renderer.render(it) }
                .subscribe(html)
    }

    private fun parseMarkdown(text: String): Node = parser.parse(text)

}