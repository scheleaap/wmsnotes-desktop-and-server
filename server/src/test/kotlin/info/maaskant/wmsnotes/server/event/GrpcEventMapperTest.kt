package info.maaskant.wmsnotes.server.event

import com.google.protobuf.ByteString
import info.maaskant.wmsnotes.model.AttachmentAddedEvent
import info.maaskant.wmsnotes.model.AttachmentDeletedEvent
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import info.maaskant.wmsnotes.server.command.grpc.Event
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrpcEventMapperTest {

    private lateinit var mapper: GrpcEventMapper

    @BeforeEach
    fun init() {
        mapper = GrpcEventMapper()
    }

    @TestFactory
    fun test(): List<DynamicTest> {
        val items = mapOf(
                NoteCreatedEvent(eventId = 1, noteId = "note", revision = 1, title = "Title")
                        to with(Event.GetEventsResponse.newBuilder()) {
                    eventId = 1
                    noteId = "note"
                    revision = 1
                    noteCreatedBuilder.title = "Title"
                    build()
                },
                NoteDeletedEvent(eventId = 1, noteId = "note", revision = 1)
                        to with(Event.GetEventsResponse.newBuilder()) {
                    eventId = 1
                    noteId = "note"
                    revision = 1
                    noteDeletedBuilder.build()
                    build()
                },
                AttachmentAddedEvent(eventId = 1, noteId = "note", revision = 1, name = "att", content = "data".toByteArray())
                        to with(Event.GetEventsResponse.newBuilder()) {
                    eventId = 1
                    noteId = "note"
                    revision = 1
                    attachmentAddedBuilder.name = "att"
                    attachmentAddedBuilder.content = ByteString.copyFrom("data".toByteArray())
                    build()
                },
                AttachmentDeletedEvent(eventId = 1, noteId = "note", revision = 1, name = "att")
                        to with(Event.GetEventsResponse.newBuilder()) {
                    eventId = 1
                    noteId = "note"
                    revision = 1
                    attachmentDeletedBuilder.name = "att"
                    build()
                }
                // Add more classes here
        )
        return items.map { (event, expectedResponse) ->
            DynamicTest.dynamicTest(event::class.simpleName) {
                // When
                val actualResponse = mapper.toGrpcGetEventsResponse(event)

                // Then
                assertThat(actualResponse).isEqualTo(expectedResponse)
            }
        }
    }

}