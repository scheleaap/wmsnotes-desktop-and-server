package info.maaskant.wmsnotes.client.api

import com.google.protobuf.ByteString
import info.maaskant.wmsnotes.model.folder.FolderCommand
import info.maaskant.wmsnotes.model.note.*
import info.maaskant.wmsnotes.server.command.grpc.Command
import javax.inject.Inject

class GrpcCommandMapper @Inject constructor() {
    fun toGrpcPostCommandRequest(command: info.maaskant.wmsnotes.model.Command): Command.PostCommandRequest {
        return when (command) { // Assign to variable to force a compilation error if 'when' expression is not exhaustive.
            is NoteCommand -> mapNoteCommand(command)
            is FolderCommand -> mapFolderCommand(command)
            else -> throw IllegalArgumentException()
        }
    }

    private fun mapFolderCommand(command: FolderCommand): Command.PostCommandRequest {
        TODO()
    }

    private fun mapNoteCommand(command: NoteCommand): Command.PostCommandRequest {
        val builder = Command.PostCommandRequest.newBuilder()
        when (command) {
            is CreateNoteCommand -> builder.apply {
                aggregateId = command.aggId
                createNote = Command.PostCommandRequest.CreateNoteCommand.newBuilder().apply {
                    path = command.path.toString()
                    title = command.title
                    content = command.content
                }.build()
            }
            is DeleteNoteCommand -> builder.apply {
                aggregateId = command.aggId
                lastRevision = command.lastRevision
                deleteNote = Command.PostCommandRequest.DeleteNoteCommand.newBuilder().build()
            }
            is UndeleteNoteCommand -> builder.apply {
                aggregateId = command.aggId
                lastRevision = command.lastRevision
                undeleteNote = Command.PostCommandRequest.UndeleteNoteCommand.newBuilder().build()
            }
            is AddAttachmentCommand -> builder.apply {
                aggregateId = command.aggId
                lastRevision = command.lastRevision
                addAttachment = Command.PostCommandRequest.AddAttachmentCommand.newBuilder().apply {
                    name = command.name
                    content = ByteString.copyFrom(command.content)
                }.build()
            }
            is DeleteAttachmentCommand -> builder.apply {
                aggregateId = command.aggId
                lastRevision = command.lastRevision
                deleteAttachment = Command.PostCommandRequest.DeleteAttachmentCommand.newBuilder().apply {
                    name = command.name
                }.build()
            }
            is ChangeContentCommand -> builder.apply {
                aggregateId = command.aggId
                lastRevision = command.lastRevision
                changeContent = Command.PostCommandRequest.ChangeContentCommand.newBuilder().apply {
                    content = command.content
                }.build()
            }
            is ChangeTitleCommand -> builder.apply {
                aggregateId = command.aggId
                lastRevision = command.lastRevision
                changeTitle = Command.PostCommandRequest.ChangeTitleCommand.newBuilder().apply {
                    title = command.title
                }.build()
            }
            is MoveCommand -> builder.apply {
                aggregateId = command.aggId
                lastRevision = command.lastRevision
                move = Command.PostCommandRequest.MoveCommand.newBuilder().apply {
                    path = command.path.toString()
                }.build()
            }
        }
        return builder.build()
    }
}