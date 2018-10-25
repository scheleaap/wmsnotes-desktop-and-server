package info.maaskant.wmsnotes.client.api

import info.maaskant.wmsnotes.model.AttachmentAddedEvent
import info.maaskant.wmsnotes.model.AttachmentDeletedEvent
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import info.maaskant.wmsnotes.server.command.grpc.Event
import javax.inject.Inject

class GrpcEventMapper @Inject constructor() {
    fun toModelClass(response: Event.GetEventsResponse): info.maaskant.wmsnotes.model.Event {
        with(response) {
            if (eventId == 0) throw IllegalArgumentException()
            if (noteId.isEmpty()) throw IllegalArgumentException("Event $eventId")

            return when (eventCase!!) {
                Event.GetEventsResponse.EventCase.NOTE_CREATED -> NoteCreatedEvent(
                        eventId = eventId,
                        noteId = noteId,
                        revision = revision,
                        title = noteCreated.title
                )
                Event.GetEventsResponse.EventCase.NOTE_DELETED -> NoteDeletedEvent(
                        eventId = eventId,
                        revision = revision,
                        noteId = noteId
                )
                Event.GetEventsResponse.EventCase.ATTACHMENT_ADDED -> AttachmentAddedEvent(
                        eventId = eventId,
                        revision = revision,
                        noteId = noteId,
                        name = attachmentAdded.name,
                        content = attachmentAdded.content.toByteArray()
                )
                Event.GetEventsResponse.EventCase.ATTACHMENT_DELETED -> AttachmentDeletedEvent(
                        eventId = eventId,
                        revision = revision,
                        noteId = noteId,
                        name = attachmentAdded.name
                )
                Event.GetEventsResponse.EventCase.EVENT_NOT_SET -> throw IllegalArgumentException("Event $eventId")
            }
        }
    }
}