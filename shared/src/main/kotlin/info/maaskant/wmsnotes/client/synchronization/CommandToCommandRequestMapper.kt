package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.CommandOrigin
import info.maaskant.wmsnotes.model.CommandRequest
import info.maaskant.wmsnotes.model.folder.FolderCommand
import info.maaskant.wmsnotes.model.folder.FolderCommandRequest
import info.maaskant.wmsnotes.model.note.NoteCommand
import info.maaskant.wmsnotes.model.note.NoteCommandRequest

class CommandToCommandRequestMapper {
    fun map(command: Command, lastRevision: Int?, origin: CommandOrigin): CommandRequest<Command> {
        return when (command) {
            is FolderCommand -> FolderCommandRequest.of(
                    command = command,
                    lastRevision = lastRevision,
                    origin = origin
            )
            is NoteCommand -> NoteCommandRequest.of(
                    command = command,
                    lastRevision = lastRevision,
                    origin = origin
            )
            else -> throw IllegalArgumentException(command.toString())
        }
    }
}