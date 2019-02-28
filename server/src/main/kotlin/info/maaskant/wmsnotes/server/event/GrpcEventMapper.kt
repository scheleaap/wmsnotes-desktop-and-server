package info.maaskant.wmsnotes.server.event

import com.google.protobuf.ByteString
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderDeletedEvent
import info.maaskant.wmsnotes.model.note.*
import info.maaskant.wmsnotes.server.command.grpc.Event
import org.springframework.stereotype.Service
import javax.inject.Inject
import javax.inject.Singleton

@Service
@Singleton
class GrpcEventMapper @Inject constructor() {
    fun toGrpcGetEventsResponse(event: info.maaskant.wmsnotes.model.Event): Event.GetEventsResponse {
        val builder = Event.GetEventsResponse.newBuilder()
                .setEventId(event.eventId)
                .setAggregateId(event.aggId)
                .setRevision(event.revision)

        @Suppress("UNUSED_VARIABLE")
        val a: Any = when (event) { // Assign to variable to force a compilation error if 'when' expression is not exhaustive.
            is NoteCreatedEvent -> builder.apply {
                noteCreated = Event.GetEventsResponse.NoteCreatedEvent.newBuilder().apply {
                    path = event.path.toString()
                    title = event.title
                    content = event.content
                }.build()
            }
            is NoteDeletedEvent -> builder.apply {
                noteDeleted = Event.GetEventsResponse.NoteDeletedEvent.newBuilder().build()
            }
            is NoteUndeletedEvent -> builder.apply {
                noteUndeleted = Event.GetEventsResponse.NoteUndeletedEvent.newBuilder().build()
            }
            is AttachmentAddedEvent -> builder.apply {
                attachmentAdded = Event.GetEventsResponse.AttachmentAddedEvent.newBuilder().apply {
                    name = event.name
                    content = ByteString.copyFrom(event.content)
                }.build()
            }
            is AttachmentDeletedEvent -> builder.apply {
                attachmentDeleted = Event.GetEventsResponse.AttachmentDeletedEvent.newBuilder().apply {
                    name = event.name
                }.build()
            }
            is ContentChangedEvent -> builder.apply {
                contentChanged = Event.GetEventsResponse.ContentChangedEvent.newBuilder().apply {
                    content = event.content
                }.build()
            }
            is TitleChangedEvent -> builder.apply {
                titleChanged = Event.GetEventsResponse.TitleChangedEvent.newBuilder().apply {
                    title = event.title
                }.build()
            }
            is MovedEvent -> builder.apply {
                moved = Event.GetEventsResponse.MovedEvent.newBuilder().apply {
                    path = event.path.toString()
                }.build()
            }
            is FolderCreatedEvent -> TODO()
            is FolderDeletedEvent -> TODO()
            else -> IllegalArgumentException()
        }
        return builder.build()
    }
}