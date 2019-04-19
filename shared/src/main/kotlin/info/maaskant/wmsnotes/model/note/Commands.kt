package info.maaskant.wmsnotes.model.note

import au.com.console.kassava.SupportsMixedTypeEquality
import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinToString
import info.maaskant.wmsnotes.model.AggregateCommand
import info.maaskant.wmsnotes.model.Path
import java.util.*

sealed class NoteCommand(aggId: String) : AggregateCommand(aggId), SupportsMixedTypeEquality

class CreateNoteCommand(aggId: String, val path: Path, val title: String, val content: String) : NoteCommand(aggId) {
    override fun toString() = kotlinToString(properties = arrayOf(CreateNoteCommand::aggId, CreateNoteCommand::path, CreateNoteCommand::title, CreateNoteCommand::content))

    override fun canEqual(other: Any?) = other is CreateNoteCommand

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(CreateNoteCommand::path, CreateNoteCommand::title, CreateNoteCommand::content),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(path, title, content, super.hashCode())
}

class DeleteNoteCommand(aggId: String, val lastRevision: Int) : NoteCommand(aggId) {
    override fun toString() = kotlinToString(properties = arrayOf(DeleteNoteCommand::aggId, DeleteNoteCommand::lastRevision))

    override fun canEqual(other: Any?) = other is DeleteNoteCommand

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(DeleteNoteCommand::lastRevision),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(lastRevision, super.hashCode())
}

class UndeleteNoteCommand(aggId: String, val lastRevision: Int) : NoteCommand(aggId) {
    override fun toString() = kotlinToString(properties = arrayOf(UndeleteNoteCommand::aggId, UndeleteNoteCommand::lastRevision))

    override fun canEqual(other: Any?) = other is UndeleteNoteCommand

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(UndeleteNoteCommand::lastRevision),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(lastRevision, super.hashCode())
}

class AddAttachmentCommand(aggId: String, val lastRevision: Int, val name: String, val content: ByteArray) : NoteCommand(aggId) {
    private val contentLength = content.size

    override fun toString() = kotlinToString(properties = arrayOf(AddAttachmentCommand::aggId, AddAttachmentCommand::lastRevision, AddAttachmentCommand::name, AddAttachmentCommand::contentLength))

    override fun canEqual(other: Any?) = other is AddAttachmentCommand

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

class DeleteAttachmentCommand(aggId: String, val lastRevision: Int, val name: String) : NoteCommand(aggId) {
    override fun toString() = kotlinToString(properties = arrayOf(DeleteAttachmentCommand::aggId, DeleteAttachmentCommand::lastRevision, DeleteAttachmentCommand::name))

    override fun canEqual(other: Any?) = other is DeleteAttachmentCommand

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(DeleteAttachmentCommand::lastRevision, DeleteAttachmentCommand::name),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(lastRevision, name, super.hashCode())
}

class ChangeContentCommand(aggId: String, val lastRevision: Int, val content: String) : NoteCommand(aggId) {
    override fun toString() = kotlinToString(properties = arrayOf(ChangeContentCommand::aggId, ChangeContentCommand::lastRevision, ChangeContentCommand::content))

    override fun canEqual(other: Any?) = other is ChangeContentCommand

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(ChangeContentCommand::lastRevision, ChangeContentCommand::content),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(lastRevision, content, super.hashCode())
}

class ChangeTitleCommand(aggId: String, val lastRevision: Int, val title: String) : NoteCommand(aggId) {
    override fun toString() = kotlinToString(properties = arrayOf(ChangeTitleCommand::aggId, ChangeTitleCommand::lastRevision, ChangeTitleCommand::title))

    override fun canEqual(other: Any?) = other is ChangeTitleCommand

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(ChangeTitleCommand::lastRevision, ChangeTitleCommand::title),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(lastRevision, title, super.hashCode())
}

class MoveCommand(aggId: String, val lastRevision: Int, val path: Path) : NoteCommand(aggId) {
    override fun toString() = kotlinToString(properties = arrayOf(MoveCommand::aggId, MoveCommand::lastRevision, MoveCommand::path))

    override fun canEqual(other: Any?) = other is MoveCommand

    override fun equals(other: Any?) = kotlinEquals(
            other = other,
            properties = arrayOf(MoveCommand::lastRevision, MoveCommand::path),
            superEquals = { super.equals(other) }
    )

    override fun hashCode() = Objects.hash(lastRevision, path, super.hashCode())
}
