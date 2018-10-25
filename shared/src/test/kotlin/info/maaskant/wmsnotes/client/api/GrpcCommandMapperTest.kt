package info.maaskant.wmsnotes.client.api

import com.google.protobuf.ByteString
import info.maaskant.wmsnotes.model.AddAttachmentCommand
import info.maaskant.wmsnotes.model.CreateNoteCommand
import info.maaskant.wmsnotes.model.DeleteAttachmentCommand
import info.maaskant.wmsnotes.model.DeleteNoteCommand
import info.maaskant.wmsnotes.server.command.grpc.Command
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

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
                CreateNoteCommand(noteId = "note", title = "Title") to with(Command.PostCommandRequest.newBuilder()) {
                    noteId = "note"
                    createNoteBuilder.title = "Title"
                    build()
                },
                DeleteNoteCommand(noteId = "note", lastRevision = 1) to with(Command.PostCommandRequest.newBuilder()) {
                    noteId = "note"
                    lastRevision = 1
                    deleteNoteBuilder.build()
                    build()
                },
                AddAttachmentCommand(noteId = "note", lastRevision = 1, name = "att", content = "data".toByteArray()) to with(Command.PostCommandRequest.newBuilder()) {
                    noteId = "note"
                    lastRevision = 1
                    addAttachmentBuilder.name = "att"
                    addAttachmentBuilder.content = ByteString.copyFrom("data".toByteArray())
                    build()
                },
                DeleteAttachmentCommand(noteId = "note", lastRevision = 1, name = "att") to with(Command.PostCommandRequest.newBuilder()) {
                    noteId = "note"
                    lastRevision = 1
                    deleteAttachmentBuilder.name = "att"
                    build()
                }
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