package info.maaskant.wmsnotes.desktop.main.editing.preview

import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.html.IndependentLinkResolverFactory
import com.vladsch.flexmark.html.LinkResolver
import com.vladsch.flexmark.html.renderer.LinkResolverContext
import com.vladsch.flexmark.html.renderer.ResolvedLink

class AttachmentLinkResolver : LinkResolver {
    override fun resolveLink(node: Node, context: LinkResolverContext, link: ResolvedLink): ResolvedLink =
            if (link.url.startsWith(attachmentPrefix)) {
                link.withUrl(link.url.substring(attachmentPrefix.length))
            } else {
                link
            }

    companion object {
        const val attachmentPrefix = "attachment:"
    }
}

class AttachmentLinkResolverFactory : IndependentLinkResolverFactory() {
    override fun create(context: LinkResolverContext?): LinkResolver =
            AttachmentLinkResolver()
}