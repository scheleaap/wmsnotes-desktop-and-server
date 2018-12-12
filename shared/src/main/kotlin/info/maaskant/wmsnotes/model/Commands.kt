package info.maaskant.wmsnotes.model

import au.com.console.kassava.kotlinToString
import java.util.*

sealed class Command

data class CreateNoteCommand(val noteId: String?, val title: String) : Command()
data class DeleteNoteCommand(val noteId: String, val lastRevision: Int) : Command()

data class AddAttachmentCommand(val noteId: String, val lastRevision: Int, val name: String, val content: ByteArray) : Command() {
    private val contentLength = content.size

    override fun toString() = kotlinToString(properties = arrayOf(AddAttachmentCommand::noteId, AddAttachmentCommand::lastRevision, AddAttachmentCommand::name, AddAttachmentCommand::contentLength))

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

data class ChangeContentCommand(val noteId: String, val lastRevision: Int, val content: String) : Command()

data class ChangeTitleCommand(val noteId: String, val lastRevision: Int, val title: String) : Command()
