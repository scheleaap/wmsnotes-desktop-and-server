package info.maaskant.wmsnotes.server.command

import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.note.*
import info.maaskant.wmsnotes.server.command.grpc.Command
import org.springframework.stereotype.Service
import javax.inject.Singleton

@Service
@Singleton
class GrpcCommandMapper {
    fun toModelCommand(request: Command.PostCommandRequest): info.maaskant.wmsnotes.model.Command {
        if (request.aggregateId.isEmpty()) throw BadRequestException("Field 'note_id' must not be empty")
        return when (request.commandCase!!) {
            Command.PostCommandRequest.CommandCase.COMMAND_NOT_SET -> throw BadRequestException("Field 'command' not set")
            Command.PostCommandRequest.CommandCase.CREATE_NOTE -> CreateNoteCommand(
                    aggId = request.aggregateId,
                    path = Path.from(request.createNote.path),
                    title = request.createNote.title,
                    content = request.createNote.content
            )
            Command.PostCommandRequest.CommandCase.DELETE_NOTE -> DeleteNoteCommand(
                    aggId = request.aggregateId,
                    lastRevision = request.lastRevision
            )
            Command.PostCommandRequest.CommandCase.UNDELETE_NOTE -> UndeleteNoteCommand(
                    aggId = request.aggregateId,
                    lastRevision = request.lastRevision
            )
            Command.PostCommandRequest.CommandCase.ADD_ATTACHMENT -> AddAttachmentCommand(
                    aggId = request.aggregateId,
                    lastRevision = request.lastRevision,
                    name = request.addAttachment.name.also { if (it.isEmpty()) throw BadRequestException("Field 'name' not set") },
                    content = request.addAttachment.content.toByteArray()
            )
            Command.PostCommandRequest.CommandCase.DELETE_ATTACHMENT -> DeleteAttachmentCommand(
                    aggId = request.aggregateId,
                    lastRevision = request.lastRevision,
                    name = request.deleteAttachment.name.also { if (it.isEmpty()) throw BadRequestException("Field 'name' not set") }
            )

            Command.PostCommandRequest.CommandCase.CHANGE_CONTENT -> ChangeContentCommand(
                    aggId = request.aggregateId,
                    lastRevision = request.lastRevision,
                    content = request.changeContent.content
            )
            Command.PostCommandRequest.CommandCase.CHANGE_TITLE -> ChangeTitleCommand(
                    aggId = request.aggregateId,
                    lastRevision = request.lastRevision,
                    title = request.changeTitle.title
            )
            Command.PostCommandRequest.CommandCase.MOVE -> MoveCommand(
                    aggId = request.aggregateId,
                    lastRevision = request.lastRevision,
                    path = Path.from(request.move.path)
            )

        }
    }
}

class BadRequestException(description: String) : Exception(description)
