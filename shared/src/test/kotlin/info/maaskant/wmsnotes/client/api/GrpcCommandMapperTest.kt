package info.maaskant.wmsnotes.client.api

import com.google.protobuf.ByteString
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.folder.CreateFolderCommand
import info.maaskant.wmsnotes.model.folder.DeleteFolderCommand
import info.maaskant.wmsnotes.model.folder.Folder
import info.maaskant.wmsnotes.model.note.*
import info.maaskant.wmsnotes.server.command.grpc.Command
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrpcCommandMapperTest {

    private lateinit var mapper: GrpcCommandMapper

    @BeforeEach
    fun init() {
        mapper = GrpcCommandMapper()
    }

    @TestFactory
    fun test(): List<DynamicTest> {
        val items = mapOf(
                CreateNoteCommand(aggId = "note", path = Path("el1", "el2"), title = "Title", content = "Text") to Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    createNote = Command.PostCommandRequest.CreateNoteCommand.newBuilder().apply {
                        path = Path("el1", "el2").toString()
                        title = "Title"
                        content = "Text"
                    }.build()
                }.build(),
                DeleteNoteCommand(aggId = "note", lastRevision = 1) to Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    deleteNote = Command.PostCommandRequest.DeleteNoteCommand.newBuilder().build()
                }.build(),
                UndeleteNoteCommand(aggId = "note", lastRevision = 1) to Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    undeleteNote = Command.PostCommandRequest.UndeleteNoteCommand.newBuilder().build()
                }.build(),
                AddAttachmentCommand(aggId = "note", lastRevision = 1, name = "att", content = "data".toByteArray()) to Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    addAttachment = Command.PostCommandRequest.AddAttachmentCommand.newBuilder().apply {
                        name = "att"
                        content = ByteString.copyFrom("data".toByteArray())
                    }.build()
                }.build(),
                DeleteAttachmentCommand(aggId = "note", lastRevision = 1, name = "att") to Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    deleteAttachment = Command.PostCommandRequest.DeleteAttachmentCommand.newBuilder().apply {
                        name = "att"
                    }.build()
                }.build(),
                ChangeContentCommand(aggId = "note", lastRevision = 1, content = "Text") to Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    changeContent = Command.PostCommandRequest.ChangeContentCommand.newBuilder().apply {
                        content = "Text"
                    }.build()
                }.build(),
                ChangeTitleCommand(aggId = "note", lastRevision = 1, title = "Title") to Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    changeTitle = Command.PostCommandRequest.ChangeTitleCommand.newBuilder().apply {
                        title = "Title"
                    }.build()
                }.build(),
                MoveCommand(aggId = "note", lastRevision = 1, path = Path("el1", "el2")) to Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    move = Command.PostCommandRequest.MoveCommand.newBuilder().apply {
                        path = Path("el1", "el2").toString()
                    }.build()
                }.build(),
                CreateFolderCommand(path = Path("el1", "el2"), lastRevision = 1) to Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = Path("el1", "el2").toString()
                    createFolder = Command.PostCommandRequest.CreateFolderCommand.newBuilder().build()
                }.build(),
                DeleteFolderCommand(path = Path("el1", "el2"), lastRevision = 1) to Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = Path("el1", "el2").toString()
                    lastRevision = 1
                    deleteFolder = Command.PostCommandRequest.DeleteFolderCommand.newBuilder().build()
                }.build()
                // Add more classes here
        )
        return items.map { (command, expectedRequest) ->
            DynamicTest.dynamicTest(command::class.simpleName) {
                // When
                val actualRequest = mapper.toGrpcPostCommandRequest(command)

                // Then
                assertThat(actualRequest).isEqualTo(expectedRequest)
            }
        }
    }

}