package info.maaskant.wmsnotes.client.api

import com.google.protobuf.ByteString
import info.maaskant.wmsnotes.model.AddAttachmentCommand
import info.maaskant.wmsnotes.model.CreateNoteCommand
import info.maaskant.wmsnotes.model.DeleteAttachmentCommand
import info.maaskant.wmsnotes.model.DeleteNoteCommand
import info.maaskant.wmsnotes.server.command.grpc.Command
import javax.inject.Inject

class GrpcCommandMapper @Inject constructor() {
    fun toGrpcPostCommandRequest(command: info.maaskant.wmsnotes.model.Command): Command.PostCommandRequest {
        val builder = Command.PostCommandRequest.newBuilder()
        @Suppress("UNUSED_VARIABLE")
        val a: Any = when (command) { // Assign to variable to force a compilation error if 'when' expression is not exhaustive.
            is CreateNoteCommand -> {
                builder.noteId = command.noteId
                builder.createNoteBuilder.title = command.title
            }
            is DeleteNoteCommand -> {
                builder.noteId = command.noteId
                builder.lastRevision = command.lastRevision
                builder.deleteNoteBuilder.build()
            }
            is AddAttachmentCommand -> {
                builder.noteId = command.noteId
                builder.lastRevision = command.lastRevision
                builder.addAttachmentBuilder.name = command.name
                builder.addAttachmentBuilder.content = ByteString.copyFrom(command.content)
            }
            is DeleteAttachmentCommand -> {
                builder.noteId = command.noteId
                builder.lastRevision = command.lastRevision
                builder.deleteAttachmentBuilder.name = command.name
            }
        }
        return builder.build()
    }
}