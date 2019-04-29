package info.maaskant.wmsnotes.server.command

import info.maaskant.wmsnotes.model.CommandRequest
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.folder.CreateFolderCommand
import info.maaskant.wmsnotes.model.folder.DeleteFolderCommand
import info.maaskant.wmsnotes.model.folder.FolderCommandRequest
import info.maaskant.wmsnotes.model.note.*
import info.maaskant.wmsnotes.server.command.grpc.Command
import org.springframework.stereotype.Service
import javax.inject.Singleton

@Service
@Singleton
class GrpcCommandMapper {
    fun toModelCommandRequest(request: Command.PostCommandRequest): CommandRequest<info.maaskant.wmsnotes.model.Command> {
        if (request.aggregateId.isEmpty()) throw BadRequestException("Field 'note_id' must not be empty")
        return when (request.commandCase!!) {
            Command.PostCommandRequest.CommandCase.COMMAND_NOT_SET -> throw BadRequestException("Field 'command' not set")
            Command.PostCommandRequest.CommandCase.CREATE_NOTE -> NoteCommandRequest.of(
                    command = CreateNoteCommand(
                            aggId = request.aggregateId,
                            path = Path.from(request.createNote.path),
                            title = request.createNote.title,
                            content = request.createNote.content
                    ),
                    lastRevision = null
            )
            Command.PostCommandRequest.CommandCase.DELETE_NOTE -> NoteCommandRequest.of(
                    command = DeleteNoteCommand(
                            aggId = request.aggregateId
                    ),
                    lastRevision = request.lastRevision
            )
            Command.PostCommandRequest.CommandCase.UNDELETE_NOTE -> NoteCommandRequest.of(
                    command = UndeleteNoteCommand(
                            aggId = request.aggregateId
                    ),
                    lastRevision = request.lastRevision
            )
            Command.PostCommandRequest.CommandCase.ADD_ATTACHMENT -> NoteCommandRequest.of(
                    command = AddAttachmentCommand(
                            aggId = request.aggregateId,
                            name = request.addAttachment.name.also { if (it.isEmpty()) throw BadRequestException("Field 'name' not set") },
                            content = request.addAttachment.content.toByteArray()
                    ),
                    lastRevision = request.lastRevision
            )
            Command.PostCommandRequest.CommandCase.DELETE_ATTACHMENT -> NoteCommandRequest.of(
                    command = DeleteAttachmentCommand(
                            aggId = request.aggregateId,
                            name = request.deleteAttachment.name.also { if (it.isEmpty()) throw BadRequestException("Field 'name' not set") }
                    ),
                    lastRevision = request.lastRevision
            )
            Command.PostCommandRequest.CommandCase.CHANGE_CONTENT -> NoteCommandRequest.of(
                    command = ChangeContentCommand(
                            aggId = request.aggregateId,
                            content = request.changeContent.content
                    ),
                    lastRevision = request.lastRevision
            )
            Command.PostCommandRequest.CommandCase.CHANGE_TITLE -> NoteCommandRequest.of(
                    command = ChangeTitleCommand(
                            aggId = request.aggregateId,
                            title = request.changeTitle.title
                    ),
                    lastRevision = request.lastRevision
            )
            Command.PostCommandRequest.CommandCase.MOVE -> NoteCommandRequest.of(
                    command = MoveCommand(
                            aggId = request.aggregateId,
                            path = Path.from(request.move.path)
                    ),
                    lastRevision = request.lastRevision
            )
            Command.PostCommandRequest.CommandCase.CREATE_FOLDER -> FolderCommandRequest.of(
                    command = CreateFolderCommand(
                            path = Path.from(request.aggregateId)
                    ),
                    lastRevision = request.lastRevision
            )
            Command.PostCommandRequest.CommandCase.DELETE_FOLDER -> FolderCommandRequest.of(
                    command = DeleteFolderCommand(
                            path = Path.from(request.aggregateId)
                    ),
                    lastRevision = request.lastRevision
            )
        }
    }
}

class BadRequestException(description: String) : Exception(description)
