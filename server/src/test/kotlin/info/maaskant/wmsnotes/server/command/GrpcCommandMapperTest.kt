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
                with(Command.PostCommandRequest.newBuilder()) {
                    // noteId
                    createNoteBuilder.title = "Title"
                    build()
                },
                with(Command.PostCommandRequest.newBuilder()) {
                    // noteId
                    lastRevision = 1
                    deleteNoteBuilder.build()
                    build()
                }
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
                with(Command.PostCommandRequest.newBuilder()) {
                    noteId = "note"
                    createNoteBuilder.title = "Title"
                    build()
                } to CreateNoteCommand(noteId = "note", title = "Title"),
                with(Command.PostCommandRequest.newBuilder()) {
                    noteId = "note"
                    lastRevision = 1
                    deleteNoteBuilder.build()
                    build()
                } to DeleteNoteCommand(noteId = "note", lastRevision = 1),
                with(Command.PostCommandRequest.newBuilder()) {
                    noteId = "note"
                    lastRevision = 1
                    addAttachmentBuilder.name = "att"
                    addAttachmentBuilder.content = ByteString.copyFrom("data".toByteArray())
                    build()
                } to AddAttachmentCommand(noteId = "note", lastRevision = 1, name = "att", content = "data".toByteArray()),
                with(Command.PostCommandRequest.newBuilder()) {
                    noteId = "note"
                    lastRevision = 1
                    deleteAttachmentBuilder.name = "att"
                    build()
                } to DeleteAttachmentCommand(noteId = "note", lastRevision = 1, name = "att")
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