package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.CommandRequest
import info.maaskant.wmsnotes.model.folder.FolderCommand
import info.maaskant.wmsnotes.model.folder.FolderCommandRequest
import info.maaskant.wmsnotes.model.note.NoteCommand
import info.maaskant.wmsnotes.model.note.NoteCommandRequest

class CommandToCommandRequestMapper {
    fun map(command: Command, lastRevision: Int?): CommandRequest<Command> {
        return when (command) {
            is FolderCommand -> FolderCommandRequest(
                    aggId = command.aggId,
                    commands = listOf(command),
                    lastRevision = lastRevision
            )
            is NoteCommand -> NoteCommandRequest(
                    aggId = command.aggId,
                    commands = listOf(command),
                    lastRevision = lastRevision
            )
            else -> throw IllegalArgumentException(command.toString())
        }
    }
}