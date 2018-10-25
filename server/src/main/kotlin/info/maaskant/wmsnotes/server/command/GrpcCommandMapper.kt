package info.maaskant.wmsnotes.server.command

import info.maaskant.wmsnotes.model.AddAttachmentCommand
import info.maaskant.wmsnotes.model.CreateNoteCommand
import info.maaskant.wmsnotes.model.DeleteAttachmentCommand
import info.maaskant.wmsnotes.model.DeleteNoteCommand
import info.maaskant.wmsnotes.server.command.grpc.Command
import org.springframework.stereotype.Service
import javax.inject.Singleton

@Service
@Singleton
class GrpcCommandMapper {
    fun toModelCommand(request: Command.PostCommandRequest): info.maaskant.wmsnotes.model.Command {
        if (request.noteId.isEmpty()) throw BadRequestException("Field 'note_id' must not be empty")
        return when (request.commandCase!!) {
            Command.PostCommandRequest.CommandCase.COMMAND_NOT_SET -> throw BadRequestException("Field 'command' not set")
            Command.PostCommandRequest.CommandCase.CREATE_NOTE -> CreateNoteCommand(
                    noteId = request.noteId,
                    title = request.createNote.title
            )
            Command.PostCommandRequest.CommandCase.DELETE_NOTE -> DeleteNoteCommand(
                    noteId = request.noteId,
                    lastRevision = request.lastRevision
            )
            Command.PostCommandRequest.CommandCase.ADD_ATTACHMENT -> AddAttachmentCommand(
                    noteId = request.noteId,
                    lastRevision = request.lastRevision,
                    name = request.addAttachment.name,
                    content = request.addAttachment.content.toByteArray()
            )
            Command.PostCommandRequest.CommandCase.DELETE_ATTACHMENT -> DeleteAttachmentCommand(
                    noteId = request.noteId,
                    lastRevision = request.lastRevision,
                    name = request.deleteAttachment.name
            )

        }
    }
}

class BadRequestException(description: String) : Exception(description)
