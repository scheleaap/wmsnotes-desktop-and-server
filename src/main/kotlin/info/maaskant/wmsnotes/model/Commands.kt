package info.maaskant.wmsnotes.model

sealed class Command

data class CreateNoteCommand(val id: String, val title: String) : Command()
data class DeleteNoteCommand(val id: String) : Command()
