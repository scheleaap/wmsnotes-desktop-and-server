package info.maaskant.wmsnotes.client.api

import com.google.protobuf.ByteString
import info.maaskant.wmsnotes.model.folder.CreateFolderCommand
import info.maaskant.wmsnotes.model.folder.DeleteFolderCommand
import info.maaskant.wmsnotes.model.folder.FolderCommand
import info.maaskant.wmsnotes.model.note.*
import info.maaskant.wmsnotes.server.command.grpc.Command
import javax.inject.Inject

class GrpcCommandMapper @Inject constructor() {
    fun toGrpcPostCommandRequest(command: info.maaskant.wmsnotes.model.Command, lastRevision: Int): Command.PostCommandRequest {
        return when (command) {
            is NoteCommand -> mapNoteCommand(command, lastRevision)
            is FolderCommand -> mapFolderCommand(command, lastRevision)
            else -> throw IllegalArgumentException()
        }
    }

    private fun mapFolderCommand(command: FolderCommand, lastRevision: Int): Command.PostCommandRequest {
        val builder = Command.PostCommandRequest.newBuilder()
        when (command) {
            is CreateFolderCommand -> builder.also {
                it.aggregateId = command.path.toString()
                it.createFolder = Command.PostCommandRequest.CreateFolderCommand.newBuilder().build()
            }
            is DeleteFolderCommand -> builder.also {
                it.aggregateId = command.path.toString()
                it.lastRevision = lastRevision
                it.deleteFolder = Command.PostCommandRequest.DeleteFolderCommand.newBuilder().build()
            }
        }
        return builder.build()
    }

    private fun mapNoteCommand(command: NoteCommand, lastRevision: Int): Command.PostCommandRequest {
        val builder = Command.PostCommandRequest.newBuilder()
        when (command) {
            is CreateNoteCommand -> builder.also {
                it.aggregateId = command.aggId
                it.createNote = Command.PostCommandRequest.CreateNoteCommand.newBuilder().also { it2 ->
                    it2.path = command.path.toString()
                    it2.title = command.title
                    it2.content = command.content
                }.build()
            }
            is DeleteNoteCommand -> builder.also {
                it.aggregateId = command.aggId
                it.lastRevision = lastRevision
                it.deleteNote = Command.PostCommandRequest.DeleteNoteCommand.newBuilder().build()
            }
            is UndeleteNoteCommand -> builder.also {
                it.aggregateId = command.aggId
                it.lastRevision = lastRevision
                it.undeleteNote = Command.PostCommandRequest.UndeleteNoteCommand.newBuilder().build()
            }
            is AddAttachmentCommand -> builder.also {
                it.aggregateId = command.aggId
                it.lastRevision = lastRevision
                it.addAttachment = Command.PostCommandRequest.AddAttachmentCommand.newBuilder().also { it2 ->
                    it2.name = command.name
                    it2.content = ByteString.copyFrom(command.content)
                }.build()
            }
            is DeleteAttachmentCommand -> builder.also {
                it.aggregateId = command.aggId
                it.lastRevision = lastRevision
                it.deleteAttachment = Command.PostCommandRequest.DeleteAttachmentCommand.newBuilder().also { it2 ->
                    it2.name = command.name
                }.build()
            }
            is ChangeContentCommand -> builder.also {
                it.aggregateId = command.aggId
                it.lastRevision = lastRevision
                it.changeContent = Command.PostCommandRequest.ChangeContentCommand.newBuilder().also { it2 ->
                    it2.content = command.content
                }.build()
            }
            is ChangeTitleCommand -> builder.also {
                it.aggregateId = command.aggId
                it.lastRevision = lastRevision
                it.changeTitle = Command.PostCommandRequest.ChangeTitleCommand.newBuilder().also { it2 ->
                    it2.title = command.title
                }.build()
            }
            is MoveCommand -> builder.also {
                it.aggregateId = command.aggId
                it.lastRevision = lastRevision
                it.move = Command.PostCommandRequest.MoveCommand.newBuilder().also { it2 ->
                    it2.path = command.path.toString()
                }.build()
            }
        }
        return builder.build()
    }
}