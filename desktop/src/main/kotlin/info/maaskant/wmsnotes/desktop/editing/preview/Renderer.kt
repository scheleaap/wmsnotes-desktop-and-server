package info.maaskant.wmsnotes.desktop.editing.preview

import com.vladsch.flexmark.ast.Node

interface Renderer {
    fun render(astRoot: Node): String
}
