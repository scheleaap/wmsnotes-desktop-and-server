package info.maaskant.wmsnotes.model.note

import au.com.console.kassava.kotlinToString
import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.Path
import java.util.*

sealed class NoteCommand : Command

data class CreateNoteCommand(val aggId: String?, val path: Path, val title: String, val content: String) : NoteCommand()
data class DeleteNoteCommand(val aggId: String, val lastRevision: Int) : NoteCommand()
data class UndeleteNoteCommand(val aggId: String, val lastRevision: Int) : NoteCommand()

data class AddAttachmentCommand(val aggId: String, val lastRevision: Int, val name: String, val content: ByteArray) : NoteCommand() {
    private val contentLength = content.size

    override fun toString() = kotlinToString(properties = arrayOf(AddAttachmentCommand::aggId, AddAttachmentCommand::lastRevision, AddAttachmentCommand::name, AddAttachmentCommand::contentLength))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AddAttachmentCommand

        if (aggId != other.aggId) return false
        if (lastRevision != other.lastRevision) return false
        if (name != other.name) return false
        if (!Arrays.equals(content, other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = aggId.hashCode()
        result = 31 * result + lastRevision
        result = 31 * result + name.hashCode()
        result = 31 * result + Arrays.hashCode(content)
        return result
    }
}

data class DeleteAttachmentCommand(val aggId: String, val lastRevision: Int, val name: String) : NoteCommand()

data class ChangeContentCommand(val aggId: String, val lastRevision: Int, val content: String) : NoteCommand()

data class ChangeTitleCommand(val aggId: String, val lastRevision: Int, val title: String) : NoteCommand()

data class MoveCommand(val aggId: String, val lastRevision: Int, val path: Path) : NoteCommand()
