package info.maaskant.wmsnotes.server.event

import com.google.protobuf.ByteString
import info.maaskant.wmsnotes.model.AttachmentAddedEvent
import info.maaskant.wmsnotes.model.AttachmentDeletedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
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
                .setNoteId(event.noteId)
                .setRevision(event.revision)

        @Suppress("UNUSED_VARIABLE")
        val a: Any = when (event) { // Assign to variable to force a compilation error if 'when' expression is not exhaustive.
            is info.maaskant.wmsnotes.model.NoteCreatedEvent -> with(builder.noteCreatedBuilder) {
                title = event.title
            }
            is NoteDeletedEvent -> with(builder.noteDeletedBuilder) {
                build()
            }
            is AttachmentAddedEvent -> with(builder.attachmentAddedBuilder) {
                name = event.name
                content = ByteString.copyFrom(event.content)
            }
            is AttachmentDeletedEvent -> with(builder.attachmentDeletedBuilder) {
                name = event.name
            }
        }
        return builder.build()
    }
}