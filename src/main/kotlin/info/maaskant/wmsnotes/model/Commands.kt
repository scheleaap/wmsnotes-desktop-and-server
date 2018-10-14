package info.maaskant.wmsnotes.model

import java.util.*

sealed class Command

data class CreateNoteCommand(val noteId: String?, val title: String) : Command()
data class DeleteNoteCommand(val noteId: String, val lastRevision: Int) : Command()

data class AddAttachmentCommand(val noteId: String, val lastRevision: Int, val name: String, val content: ByteArray) : Command() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AddAttachmentCommand

        if (noteId != other.noteId) return false
        if (lastRevision != other.lastRevision) return false
        if (name != other.name) return false
        if (!Arrays.equals(content, other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = noteId.hashCode()
        result = 31 * result + lastRevision
        result = 31 * result + name.hashCode()
        result = 31 * result + Arrays.hashCode(content)
        return result
    }
}

data class DeleteAttachmentCommand(val noteId: String, val lastRevision: Int, val name: String) : Command()
