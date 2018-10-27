package info.maaskant.wmsnotes.server.command

import com.google.protobuf.ByteString
import info.maaskant.wmsnotes.model.AddAttachmentCommand
import info.maaskant.wmsnotes.model.CreateNoteCommand
import info.maaskant.wmsnotes.model.DeleteAttachmentCommand
import info.maaskant.wmsnotes.model.DeleteNoteCommand
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
            noteId = "note"
            build()
        }

        // When
        val thrown = catchThrowable { mapper.toModelCommand(request) }

        // Then
        assertThat(thrown).isInstanceOf(BadRequestException::class.java)
    }

    @TestFactory
    fun `missing note id`(): List<DynamicTest> {
        val requests = listOf(
                Command.PostCommandRequest.newBuilder().apply {
                    // noteId
                    createNote = Command.PostCommandRequest.CreateNoteCommand.newBuilder().apply {
                        title = "Title"
                    }.build()
                }.build(),
                Command.PostCommandRequest.newBuilder().apply {
                    // noteId
                    lastRevision = 1
                    deleteNote = Command.PostCommandRequest.DeleteNoteCommand.newBuilder().build()
                }.build()                // There's no need to add a check for every command.
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
                    noteId = "note"
                    createNote = Command.PostCommandRequest.CreateNoteCommand.newBuilder().apply {
                        title = "Title"
                    }.build()
                }.build() to CreateNoteCommand(noteId = "note", title = "Title"),
                Command.PostCommandRequest.newBuilder().apply {
                    noteId = "note"
                    lastRevision = 1
                    deleteNote = Command.PostCommandRequest.DeleteNoteCommand.newBuilder().build()
                }.build() to DeleteNoteCommand(noteId = "note", lastRevision = 1),
                Command.PostCommandRequest.newBuilder().apply {
                    noteId = "note"
                    lastRevision = 1
                    addAttachment = Command.PostCommandRequest.AddAttachmentCommand.newBuilder().apply {
                        name = "att"
                        content = ByteString.copyFrom("data".toByteArray())
                    }.build()
                }.build() to AddAttachmentCommand(noteId = "note", lastRevision = 1, name = "att", content = "data".toByteArray()),
                Command.PostCommandRequest.newBuilder().apply {
                    noteId = "note"
                    lastRevision = 1
                    deleteAttachment = Command.PostCommandRequest.DeleteAttachmentCommand.newBuilder().apply {
                        name = "att"
                    }.build()
                }.build() to DeleteAttachmentCommand(noteId = "note", lastRevision = 1, name = "att")
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

}