package info.maaskant.wmsnotes.client.api

import com.google.protobuf.ByteString
import info.maaskant.wmsnotes.model.*
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
                CreateNoteCommand(noteId = "note", path = TODO(), title = "Title",content = TODO()) to Command.PostCommandRequest.newBuilder().apply {
                    noteId = "note"
                    createNote = Command.PostCommandRequest.CreateNoteCommand.newBuilder().apply {
                        title = "Title"
                    }.build()
                }.build(),
                DeleteNoteCommand(noteId = "note", lastRevision = 1) to Command.PostCommandRequest.newBuilder().apply {
                    noteId = "note"
                    lastRevision = 1
                    deleteNote = Command.PostCommandRequest.DeleteNoteCommand.newBuilder().build()
                }.build(),
                UndeleteNoteCommand(noteId = "note", lastRevision = 1) to Command.PostCommandRequest.newBuilder().apply {
                    noteId = "note"
                    lastRevision = 1
                    undeleteNote = Command.PostCommandRequest.UndeleteNoteCommand.newBuilder().build()
                }.build(),
                AddAttachmentCommand(noteId = "note", lastRevision = 1, name = "att", content = "data".toByteArray()) to Command.PostCommandRequest.newBuilder().apply {
                    noteId = "note"
                    lastRevision = 1
                    addAttachment = Command.PostCommandRequest.AddAttachmentCommand.newBuilder().apply {
                        name = "att"
                        content = ByteString.copyFrom("data".toByteArray())
                    }.build()
                }.build(),
                DeleteAttachmentCommand(noteId = "note", lastRevision = 1, name = "att") to Command.PostCommandRequest.newBuilder().apply {
                    noteId = "note"
                    lastRevision = 1
                    deleteAttachment = Command.PostCommandRequest.DeleteAttachmentCommand.newBuilder().apply {
                        name = "att"
                    }.build()
                }.build(),
                ChangeContentCommand(noteId = "note", lastRevision = 1, content = "Text") to Command.PostCommandRequest.newBuilder().apply {
                    noteId = "note"
                    lastRevision = 1
                    changeContent = Command.PostCommandRequest.ChangeContentCommand.newBuilder().apply {
                        content = "Text"
                    }.build()
                }.build(),
                ChangeTitleCommand(noteId = "note", lastRevision = 1, title = "Title") to Command.PostCommandRequest.newBuilder().apply {
                    noteId = "note"
                    lastRevision = 1
                    changeTitle = Command.PostCommandRequest.ChangeTitleCommand.newBuilder().apply {
                        title = "Title"
                    }.build()
                }.build(),
                MoveCommand(noteId = "note", lastRevision = 1, path = Path("el1", "el2")) to Command.PostCommandRequest.newBuilder().apply {
                    noteId = "note"
                    lastRevision = 1
                    move = Command.PostCommandRequest.MoveCommand.newBuilder().apply {
                        path = TODO()
                    }.build()
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