package info.maaskant.wmsnotes.client.api

import com.google.protobuf.ByteString
import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.server.command.grpc.Command
import javax.inject.Inject

class GrpcCommandMapper @Inject constructor() {
    fun toGrpcPostCommandRequest(command: info.maaskant.wmsnotes.model.Command): Command.PostCommandRequest {
        val builder = Command.PostCommandRequest.newBuilder()
        @Suppress("UNUSED_VARIABLE")
        val a: Any = when (command) { // Assign to variable to force a compilation error if 'when' expression is not exhaustive.
            is CreateNoteCommand -> builder.apply {
                noteId = command.noteId
                createNote = Command.PostCommandRequest.CreateNoteCommand.newBuilder().apply {
                    title = command.title
                }.build()
            }
            is DeleteNoteCommand -> builder.apply {
                noteId = command.noteId
                lastRevision = command.lastRevision
                deleteNote = Command.PostCommandRequest.DeleteNoteCommand.newBuilder().build()
            }
            is UndeleteNoteCommand -> builder.apply {
                noteId = command.noteId
                lastRevision = command.lastRevision
                undeleteNote = Command.PostCommandRequest.UndeleteNoteCommand.newBuilder().build()
            }
            is AddAttachmentCommand -> builder.apply {
                noteId = command.noteId
                lastRevision = command.lastRevision
                addAttachment = Command.PostCommandRequest.AddAttachmentCommand.newBuilder().apply {
                    name = command.name
                    content = ByteString.copyFrom(command.content)
                }.build()
            }
            is DeleteAttachmentCommand -> builder.apply {
                noteId = command.noteId
                lastRevision = command.lastRevision
                deleteAttachment = Command.PostCommandRequest.DeleteAttachmentCommand.newBuilder().apply {
                    name = command.name
                }.build()
            }
            is ChangeContentCommand -> builder.apply {
                noteId = command.noteId
                lastRevision = command.lastRevision
                changeContent = Command.PostCommandRequest.ChangeContentCommand.newBuilder().apply {
                    content = command.content
                }.build()
            }
            is ChangeTitleCommand -> builder.apply {
                noteId = command.noteId
                lastRevision = command.lastRevision
                changeTitle = Command.PostCommandRequest.ChangeTitleCommand.newBuilder().apply {
                    title = command.title
                }.build()
            }
            is MoveCommand -> TODO()
        }
        return builder.build()
    }
}