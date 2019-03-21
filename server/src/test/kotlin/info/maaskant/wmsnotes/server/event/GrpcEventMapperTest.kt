package info.maaskant.wmsnotes.server.event

import com.google.protobuf.ByteString
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderDeletedEvent
import info.maaskant.wmsnotes.model.note.*
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
                NoteCreatedEvent(eventId = 1, aggId = "note", revision = 1, path = Path("el1", "el2"), title = "Title", content = "Text")
                        to Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = "note"
                    revision = 1
                    noteCreated = Event.GetEventsResponse.NoteCreatedEvent.newBuilder().apply {
                        path = Path("el1", "el2").toString()
                        title = "Title"
                        content = "Text"
                    }.build()
                }.build(),
                NoteDeletedEvent(eventId = 1, aggId = "note", revision = 1)
                        to Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = "note"
                    revision = 1
                    noteDeleted = Event.GetEventsResponse.NoteDeletedEvent.newBuilder().build()
                }.build(),
                NoteUndeletedEvent(eventId = 1, aggId = "note", revision = 1)
                        to Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = "note"
                    revision = 1
                    noteUndeleted = Event.GetEventsResponse.NoteUndeletedEvent.newBuilder().build()
                }.build(),
                AttachmentAddedEvent(eventId = 1, aggId = "note", revision = 1, name = "att", content = "data".toByteArray())
                        to Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = "note"
                    revision = 1
                    attachmentAdded = Event.GetEventsResponse.AttachmentAddedEvent.newBuilder().apply {
                        name = "att"
                        content = ByteString.copyFrom("data".toByteArray())
                    }.build()
                }.build(),
                AttachmentDeletedEvent(eventId = 1, aggId = "note", revision = 1, name = "att")
                        to Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = "note"
                    revision = 1
                    attachmentDeleted = Event.GetEventsResponse.AttachmentDeletedEvent.newBuilder().apply {
                        name = "att"
                    }.build()
                }.build(),
                ContentChangedEvent(eventId = 1, aggId = "note", revision = 1, content = "Text")
                        to Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = "note"
                    revision = 1
                    contentChanged = Event.GetEventsResponse.ContentChangedEvent.newBuilder().apply {
                        content = "Text"
                    }.build()
                }.build(),
                TitleChangedEvent(eventId = 1, aggId = "note", revision = 1, title = "Title")
                        to Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = "note"
                    revision = 1
                    titleChanged = Event.GetEventsResponse.TitleChangedEvent.newBuilder().apply {
                        title = "Title"
                    }.build()
                }.build(),
                MovedEvent(eventId = 1, aggId = "note", revision = 1, path = Path("el1", "el2"))
                        to Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = "note"
                    revision = 1
                    moved = Event.GetEventsResponse.MovedEvent.newBuilder().apply {
                        path = Path("el1", "el2").toString()
                    }.build()
                }.build(),
                FolderCreatedEvent(eventId = 1, revision = 1, path = Path("el1", "el2"))
                        to Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = Path("el1", "el2").toString()
                    revision = 1
                    folderCreated = Event.GetEventsResponse.FolderCreatedEvent.newBuilder().build()
                }.build(),
                FolderDeletedEvent(eventId = 1, revision = 1, path = Path("el1", "el2"))
                        to Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = Path("el1", "el2").toString()
                    revision = 1
                    folderDeleted = Event.GetEventsResponse.FolderDeletedEvent.newBuilder().build()
                }.build()
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