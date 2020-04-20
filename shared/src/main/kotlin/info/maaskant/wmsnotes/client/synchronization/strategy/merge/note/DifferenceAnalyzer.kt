package info.maaskant.wmsnotes.client.synchronization.strategy.merge.note

import au.com.console.kassava.kotlinEquals
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.note.Note
import java.util.*

class DifferenceAnalyzer {
    fun compare(left: Note, right: Note): Set<Difference> {
        val differences: MutableSet<Difference> = mutableSetOf()

        differences += compareExistence(left, right)
        differences += comparePath(left, right)
        differences += compareTitle(left, right)
        differences += compareContent(left, right)
        differences += compareAttachments(left, right)
        return differences
    }

    private fun compareAttachments(left: Note, right: Note): Set<Difference> {
        val differences: MutableSet<Difference> = mutableSetOf()
        val leftAttachmentNames = left.attachments.keys
        val rightAttachmentNames = right.attachments.keys
        for (name in (leftAttachmentNames + rightAttachmentNames)) {
            if (left.attachmentHashes[name] != right.attachmentHashes[name]) {
                differences += AttachmentDifference(name, left.attachments[name], right.attachments[name])
            }
        }
        return differences
    }

    private fun compareContent(left: Note, right: Note): Set<Difference> =
            if (left.content != right.content) {
                setOf(ContentDifference(left.content, right.content))
            } else {
                emptySet()
            }

    private fun compareExistence(left: Note, right: Note): Set<Difference> {
        val leftStatus = getExistenceType(left)
        val rightStatus = getExistenceType(right)
        return if (leftStatus != rightStatus) {
            setOf(ExistenceDifference(leftStatus, rightStatus))
        } else {
            emptySet()
        }
    }

    private fun comparePath(left: Note, right: Note): Set<Difference> =
            if (left.path != right.path) {
                setOf(PathDifference(left.path, right.path))
            } else {
                emptySet()
            }

    private fun compareTitle(left: Note, right: Note): Set<Difference> =
            if (left.title != right.title) {
                setOf(TitleDifference(left.title, right.title))
            } else {
                emptySet()
            }

    private fun getExistenceType(note: Note) = if (note.exists) {
        ExistenceDifference.ExistenceType.EXISTS
    } else {
        if (note.revision > 0) {
            ExistenceDifference.ExistenceType.DELETED
        } else {
            ExistenceDifference.ExistenceType.NOT_YET_CREATED
        }
    }
}

sealed class Difference

data class ExistenceDifference(val left: ExistenceType, val right: ExistenceType) : Difference() {
    enum class ExistenceType {
        NOT_YET_CREATED,
        EXISTS,
        DELETED
    }
}

data class PathDifference(val left: Path, val right: Path) : Difference()

data class TitleDifference(val left: String, val right: String) : Difference() {
    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(TitleDifference::left, TitleDifference::right)
    )

    override fun hashCode() = Objects.hash(left, right)
}

data class ContentDifference(val left: String, val right: String) : Difference()
data class AttachmentDifference(val name: String, val left: ByteArray?, val right: ByteArray?) : Difference() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttachmentDifference

        if (name != other.name) return false
        if (!Arrays.equals(left, other.left)) return false
        if (!Arrays.equals(right, other.right)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (left?.contentHashCode() ?: 0)
        result = 31 * result + (right?.contentHashCode() ?: 0)
        return result
    }
}
