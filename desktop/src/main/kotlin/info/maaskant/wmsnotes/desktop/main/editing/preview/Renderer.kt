package info.maaskant.wmsnotes.desktop.main.editing.preview

import com.vladsch.flexmark.ast.Node

interface Renderer {
    fun render(astRoot: Node): String
}
