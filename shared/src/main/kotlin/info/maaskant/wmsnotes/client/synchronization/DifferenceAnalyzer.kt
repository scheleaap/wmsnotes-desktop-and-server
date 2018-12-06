package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.projection.Note

class DifferenceAnalyzer {
    fun compare(left: Note, right: Note): Set<Difference> {
        if (left.exists != right.exists) {
            return setOf(ExistenceDifference(left.exists, right.exists))
        }
        if (left.title != right.title) {
            return setOf(TitleDifference(left.title, right.title))
        }
        if (left.content != right.content) {
            return setOf(ContentDifference(left.content, right.content))
        }
        val leftAttachmentNames = left.attachments.keys
        val rightAttachmentNames = right.attachments.keys
        for (name in (leftAttachmentNames + rightAttachmentNames)) {
            if (left.attachmentHashes[name] != right.attachmentHashes[name]) {
                return setOf(AttachmentDifference(name, left.attachments[name], right.attachments[name]))
            }
        }
        return emptySet()
    }
}

sealed class Difference

data class ExistenceDifference(val left: Boolean, val right: Boolean) : Difference()
data class TitleDifference(val left: String, val right: String) : Difference()
data class ContentDifference(val left: String, val right: String) : Difference()
data class AttachmentDifference(val name: String, val left: ByteArray?, val right: ByteArray?) : Difference()
