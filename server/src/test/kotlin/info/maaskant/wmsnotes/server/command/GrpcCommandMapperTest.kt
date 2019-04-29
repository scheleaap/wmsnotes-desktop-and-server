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
        val thrown = catchThrowable { mapper.toModelCommandRequest(request) }

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
                val thrown = catchThrowable { mapper.toModelCommandRequest(request) }

                // Then
                assertThat(thrown).isInstanceOf(BadRequestException::class.java)
            }
        }
    }

    @TestFactory
    fun test(): List<DynamicTest> {
        val lastRevisionValue = 3
        val items = mapOf(
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    createNote = Command.PostCommandRequest.CreateNoteCommand.newBuilder().apply {
                        path = Path("el1", "el2").toString()
                        title = "Title"
                        content = "Text"
                    }.build()
                }.build() to (CreateNoteCommand(aggId = "note", path = Path("el1", "el2"), title = "Title", content = "Text") to null),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = lastRevisionValue
                    deleteNote = Command.PostCommandRequest.DeleteNoteCommand.newBuilder().build()
                }.build() to (DeleteNoteCommand(aggId = "note") to lastRevisionValue),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = lastRevisionValue
                    undeleteNote = Command.PostCommandRequest.UndeleteNoteCommand.newBuilder().build()
                }.build() to (UndeleteNoteCommand(aggId = "note") to lastRevisionValue),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = lastRevisionValue
                    addAttachment = Command.PostCommandRequest.AddAttachmentCommand.newBuilder().apply {
                        name = "att"
                        content = ByteString.copyFrom("data".toByteArray())
                    }.build()
                }.build() to (AddAttachmentCommand(aggId = "note", name = "att", content = "data".toByteArray()) to lastRevisionValue),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = lastRevisionValue
                    deleteAttachment = Command.PostCommandRequest.DeleteAttachmentCommand.newBuilder().apply {
                        name = "att"
                    }.build()
                }.build() to (DeleteAttachmentCommand(aggId = "note", name = "att") to lastRevisionValue),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = lastRevisionValue
                    changeContent = Command.PostCommandRequest.ChangeContentCommand.newBuilder().apply {
                        content = "Text"
                    }.build()
                }.build() to (ChangeContentCommand(aggId = "note", content = "Text") to lastRevisionValue),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = lastRevisionValue
                    changeTitle = Command.PostCommandRequest.ChangeTitleCommand.newBuilder().apply {
                        title = "Title"
                    }.build()
                }.build() to (ChangeTitleCommand(aggId = "note", title = "Title") to lastRevisionValue),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = "note"
                    lastRevision = lastRevisionValue
                    move = Command.PostCommandRequest.MoveCommand.newBuilder().apply {
                        path = Path("el1", "el2").toString()
                    }.build()
                }.build() to (MoveCommand(aggId = "note", path = Path("el1", "el2")) to lastRevisionValue),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = Path("el1", "el2").toString()
                    lastRevision = lastRevisionValue
                    createFolder = Command.PostCommandRequest.CreateFolderCommand.newBuilder().build()
                }.build() to (CreateFolderCommand(path = Path("el1", "el2")) to lastRevisionValue),
                Command.PostCommandRequest.newBuilder().apply {
                    aggregateId = Path("el1", "el2").toString()
                    lastRevision = lastRevisionValue
                    deleteFolder = Command.PostCommandRequest.DeleteFolderCommand.newBuilder().build()
                }.build() to (DeleteFolderCommand(path = Path("el1", "el2")) to lastRevisionValue)
                // Add more classes here
        )
        return items.map { (request, expectedCommandAndLastRevision) ->
            DynamicTest.dynamicTest(request.commandCase.name) {
                // Given
                val (expectedCommand, expectedLastRevision) = expectedCommandAndLastRevision

                // When
                val commandRequest = mapper.toModelCommandRequest(request)

                // Then
                assertThat(commandRequest.aggId).isEqualTo(expectedCommand.aggId)
                assertThat(commandRequest.commands).isEqualTo(listOf(expectedCommand))
                assertThat(commandRequest.lastRevision).isEqualTo(expectedLastRevision)
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
                val thrown = catchThrowable { mapper.toModelCommandRequest(request) }

                // Then
                assertThat(thrown).isInstanceOf(BadRequestException::class.java)
            }
        }
    }

}