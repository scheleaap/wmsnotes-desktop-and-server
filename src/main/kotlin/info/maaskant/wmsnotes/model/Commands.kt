package info.maaskant.wmsnotes.model

sealed class Command

data class CreateNoteCommand(val noteId: String, val title: String) : Command()
data class DeleteNoteCommand(val noteId: String) : Command()
