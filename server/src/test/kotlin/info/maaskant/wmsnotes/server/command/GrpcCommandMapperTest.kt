package info.maaskant.wmsnotes.server.command

import com.google.protobuf.ByteString
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.folder.CreateFolderCommand
import info.maaskant.wmsnotes.model.folder.DeleteFolderCommand
import info.maaskant.wmsnotes.model.note.*
import info.maaskant.wmsnotes.server.command.grpc.Command
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrpcCommandMapperTest {

    private lateinit var mapper: GrpcCommandMapper

    @BeforeEach
    fun init() {
        mapper = GrpcCommandMapper()
    }

    @Test
    fun `command not set`() {
        // Given
        val request = with(Command.PostCommandRequest.newBuilder()) {
            aggregateId = "note"
            build()
        }

        // When
        val thrown = catchThrowable { mapper.toModelCommand(request) }

        // Then
        assertThat(thrown).isInstanceOf(BadRequestException::class.java)
    }

    @TestFactory
    fun `missing aggregate id`(): List<DynamicTest> {
        val requests = listOf(
                Command.PostCommandRequest.newBuilder().apply {
                    // aggregateId
                    createNote = Command.PostCommandRequest.CreateNoteCommand.newBuilder().apply {
                        title = "Title"
                    }.build()
                }.build(),
                Command.PostCommandRequest.newBuilder().apply {
                    // aggregateId
                    lastRevision = 1
                    deleteNote = Command.PostCommandRequest.DeleteNoteCommand.newBuilder().build()
                }.build(),
                Command.PostCommandRequest.newBuilder().apply {
                    // aggregateId
                    lastRevision = 1
                    createFolder = Command.PostCommandRequest.CreateFolderCommand.newBuilder().build()
                }.build(),
                Command.PostCommandRequest.newBuilder().apply {
                    // aggregateId
                    lastRevision = 1
                    deleteFolder = Command.PostCommandRequest.DeleteFolderCommand.newBuilder().build()
                }.build()
                // There's no need to add a check for every command.
        )
        return requests.map { request ->
            DynamicTest.dynamicTest(request.commandCase.name) {
                // Given

                // When
                val thrown = catchThrowable { mapper.toModelCommand(request) }

                // Then
                assertThat(thrown).isInstanceOf(BadRequestException::class.java)
            }
        }
    }

    @TestFactory
    fun test(): List<DynamicTest> {
        val items = mapOf(
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    createNote = Command.PostCommandRequest.CreateNoteCommand.newBuilder().apply {
                        path = Path("el1", "el2").toString()
                        title = "Title"
                        content = "Text"
                    }.build()
                }.build() to CreateNoteCommand(aggId = "note", path = Path("el1", "el2"), title = "Title", content = "Text"),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    deleteNote = Command.PostCommandRequest.DeleteNoteCommand.newBuilder().build()
                }.build() to DeleteNoteCommand(aggId = "note", lastRevision = 1),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    undeleteNote = Command.PostCommandRequest.UndeleteNoteCommand.newBuilder().build()
                }.build() to UndeleteNoteCommand(aggId = "note", lastRevision = 1),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    addAttachment = Command.PostCommandRequest.AddAttachmentCommand.newBuilder().apply {
                        name = "att"
                        content = ByteString.copyFrom("data".toByteArray())
                    }.build()
                }.build() to AddAttachmentCommand(aggId = "note", lastRevision = 1, name = "att", content = "data".toByteArray()),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    deleteAttachment = Command.PostCommandRequest.DeleteAttachmentCommand.newBuilder().apply {
                        name = "att"
                    }.build()
                }.build() to DeleteAttachmentCommand(aggId = "note", lastRevision = 1, name = "att"),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    changeContent = Command.PostCommandRequest.ChangeContentCommand.newBuilder().apply {
                        content = "Text"
                    }.build()
                }.build() to ChangeContentCommand(aggId = "note", lastRevision = 1, content = "Text"),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    changeTitle = Command.PostCommandRequest.ChangeTitleCommand.newBuilder().apply {
                        title = "Title"
                    }.build()
                }.build() to ChangeTitleCommand(aggId = "note", lastRevision = 1, title = "Title"),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    move = Command.PostCommandRequest.MoveCommand.newBuilder().apply {
                        path = Path("el1", "el2").toString()
                    }.build()
                }.build() to MoveCommand(aggId = "note", lastRevision = 1, path = Path("el1", "el2")),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = Path("el1", "el2").toString()
                    lastRevision = 1
                    createFolder = Command.PostCommandRequest.CreateFolderCommand.newBuilder().build()
                }.build() to CreateFolderCommand(path = Path("el1", "el2"), lastRevision = 1),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = Path("el1", "el2").toString()
                    lastRevision = 1
                    deleteFolder = Command.PostCommandRequest.DeleteFolderCommand.newBuilder().build()
                }.build() to DeleteFolderCommand(path = Path("el1", "el2"), lastRevision = 1)
                // Add more classes here
        )
        return items.map { (request, expectedCommand) ->
            DynamicTest.dynamicTest(request.commandCase.name) {
                // When
                val actualCommand = mapper.toModelCommand(request)

                // Then
                assertThat(actualCommand).isEqualTo(expectedCommand)
            }
        }
    }

    @TestFactory
    fun `other missing fields`(): List<DynamicTest> {
        val requests = listOf(
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    addAttachment = Command.PostCommandRequest.AddAttachmentCommand.newBuilder().apply {
                        // name
                        content = ByteString.copyFrom("data".toByteArray())
                    }.build()
                }.build(),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = 1
                    deleteAttachment = Command.PostCommandRequest.DeleteAttachmentCommand.newBuilder().apply {
                        // name
                    }.build()
                }.build()
        )
        return requests.map { request ->
            DynamicTest.dynamicTest(request.commandCase.name) {
                // Given

                // When
                val thrown = catchThrowable { mapper.toModelCommand(request) }

                // Then
                assertThat(thrown).isInstanceOf(BadRequestException::class.java)
            }
        }
    }

}