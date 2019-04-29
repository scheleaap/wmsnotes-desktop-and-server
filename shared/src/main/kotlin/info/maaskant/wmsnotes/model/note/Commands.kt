package info.maaskant.wmsnotes.model.note

import au.com.console.kassava.SupportsMixedTypeEquality
import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinToString
import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.Path
import java.util.*

sealed class NoteCommand(aggId: String) : Command(aggId), SupportsMixedTypeEquality

class CreateNoteCommand(aggId: String, val path: Path, val title: String, val content: String) : NoteCommand(aggId) {
    private val contentLength = content.length

    override fun toString() = kotlinToString(properties = arrayOf(CreateNoteCommand::aggId, CreateNoteCommand::path, CreateNoteCommand::title, CreateNoteCommand::contentLength))

    override fun canEqual(other: Any?) = other is CreateNoteCommand

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(CreateNoteCommand::path, CreateNoteCommand::title, CreateNoteCommand::content),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(path, title, content, super.hashCode())
}

class DeleteNoteCommand(aggId: String) : NoteCommand(aggId) {
    override fun toString() = kotlinToString(properties = arrayOf(DeleteNoteCommand::aggId))

    override fun canEqual(other: Any?) = other is DeleteNoteCommand

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(super.hashCode())
}

class UndeleteNoteCommand(aggId: String) : NoteCommand(aggId) {
    override fun toString() = kotlinToString(properties = arrayOf(UndeleteNoteCommand::aggId))

    override fun canEqual(other: Any?) = other is UndeleteNoteCommand

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(super.hashCode())
}

class AddAttachmentCommand(aggId: String, val name: String, val content: ByteArray) : NoteCommand(aggId) {
    private val contentLength = content.size

    override fun toString() = kotlinToString(properties = arrayOf(AddAttachmentCommand::aggId, AddAttachmentCommand::name, AddAttachmentCommand::contentLength))

    override fun canEqual(other: Any?) = other is AddAttachmentCommand

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AddAttachmentCommand

        if (aggId != other.aggId) return false
        if (name != other.name) return false
        if (!Arrays.equals(content, other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = aggId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + Arrays.hashCode(content)
        return result
    }
}

class DeleteAttachmentCommand(aggId: String, val name: String) : NoteCommand(aggId) {
    override fun toString() = kotlinToString(properties = arrayOf(DeleteAttachmentCommand::aggId, DeleteAttachmentCommand::name))

    override fun canEqual(other: Any?) = other is DeleteAttachmentCommand

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(DeleteAttachmentCommand::name),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(name, super.hashCode())
}

class ChangeContentCommand(aggId: String, val content: String) : NoteCommand(aggId) {
    private val contentLength = content.length

    override fun toString() = kotlinToString(properties = arrayOf(ChangeContentCommand::aggId, ChangeContentCommand::contentLength))

    override fun canEqual(other: Any?) = other is ChangeContentCommand

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(ChangeContentCommand::content),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(content, super.hashCode())
}

class ChangeTitleCommand(aggId: String, val title: String) : NoteCommand(aggId) {
    override fun toString() = kotlinToString(properties = arrayOf(ChangeTitleCommand::aggId, ChangeTitleCommand::title))

    override fun canEqual(other: Any?) = other is ChangeTitleCommand

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(ChangeTitleCommand::title),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(title, super.hashCode())
}

class MoveCommand(aggId: String, val path: Path) : NoteCommand(aggId) {
    override fun toString() = kotlinToString(properties = arrayOf(MoveCommand::aggId, MoveCommand::path))

    override fun canEqual(other: Any?) = other is MoveCommand

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(MoveCommand::path),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(path, super.hashCode())
}
