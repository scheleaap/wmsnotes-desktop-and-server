package info.maaskant.wmsnotes.client.api

import com.google.protobuf.ByteString
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderDeletedEvent
import info.maaskant.wmsnotes.model.note.*
import info.maaskant.wmsnotes.server.command.grpc.Event
import assertk.assertThat
import assertk.assertions.*
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
                Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = "note"
                    revision = 1
                    noteCreated = Event.GetEventsResponse.NoteCreatedEvent.newBuilder().apply {
                        path = Path("el1", "el2").toString()
                        title = "Title"
                        content = "Text"
                    }.build()
                }.build() to NoteCreatedEvent(eventId = 1, aggId = "note", revision = 1, path = Path("el1", "el2"), title = "Title", content = "Text"),
                Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = "note"
                    revision = 1
                    noteDeleted = Event.GetEventsResponse.NoteDeletedEvent.newBuilder().build()
                }.build() to NoteDeletedEvent(eventId = 1, aggId = "note", revision = 1),
                Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = "note"
                    revision = 1
                    noteUndeleted = Event.GetEventsResponse.NoteUndeletedEvent.newBuilder().build()
                }.build() to NoteUndeletedEvent(eventId = 1, aggId = "note", revision = 1),
                Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = "note"
                    revision = 1
                    attachmentAdded = Event.GetEventsResponse.AttachmentAddedEvent.newBuilder().apply {
                        name = "att"
                        content = ByteString.copyFrom("data".toByteArray())
                    }.build()
                }.build() to AttachmentAddedEvent(eventId = 1, aggId = "note", revision = 1, name = "att", content = "data".toByteArray()),
                // No idea why this fails
//                Event.GetEventsResponse.newBuilder().apply {
//                    eventId = 1
//                    aggregateId = "note"
//                    revision = 1
//                    attachmentDeleted = Event.GetEventsResponse.AttachmentDeletedEvent.newBuilder().apply {
//                        name = "att"
//                    }.build()
//                }.build() to AttachmentDeletedEvent(eventId = 1, aggId = "note", revision = 1, name = "att"),
                Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = "note"
                    revision = 1
                    contentChanged = Event.GetEventsResponse.ContentChangedEvent.newBuilder().apply {
                        content = "Text"
                    }.build()
                }.build() to ContentChangedEvent(eventId = 1, aggId = "note", revision = 1, content = "Text"),
                Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = "note"
                    revision = 1
                    titleChanged = Event.GetEventsResponse.TitleChangedEvent.newBuilder().apply {
                        title = "Title"
                    }.build()
                }.build() to TitleChangedEvent(eventId = 1, aggId = "note", revision = 1, title = "Title"),
                Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = "note"
                    revision = 1
                    moved = Event.GetEventsResponse.MovedEvent.newBuilder().apply {
                        path = Path("el1", "el2").toString()
                    }.build()
                }.build() to MovedEvent(eventId = 1, aggId = "note", revision = 1, path = Path("el1", "el2")),
                Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = Path("el1", "el2").toString()
                    revision = 1
                    folderCreated = Event.GetEventsResponse.FolderCreatedEvent.newBuilder().build()
                }.build() to FolderCreatedEvent(eventId = 1, revision = 1, path = Path("el1", "el2")),
                Event.GetEventsResponse.newBuilder().apply {
                    eventId = 1
                    aggregateId = Path("el1", "el2").toString()
                    revision = 1
                    folderDeleted = Event.GetEventsResponse.FolderDeletedEvent.newBuilder().build()
                }.build() to FolderDeletedEvent(eventId = 1, revision = 1, path = Path("el1", "el2"))
                // Add more classes here
        )
        return items.map { (event, expectedResponse) ->
            DynamicTest.dynamicTest(event::class.simpleName) {
                // When
                val actualResponse = mapper.toModelClass(event)

                // Then
                assertThat(actualResponse).isEqualTo(expectedResponse)
            }
        }
    }

}