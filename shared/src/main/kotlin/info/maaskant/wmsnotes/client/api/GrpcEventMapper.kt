package info.maaskant.wmsnotes.client.api

import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderDeletedEvent
import info.maaskant.wmsnotes.model.note.*
import info.maaskant.wmsnotes.server.command.grpc.Event
import javax.inject.Inject

class GrpcEventMapper @Inject constructor() {
    fun toModelClass(response: Event.GetEventsResponse): info.maaskant.wmsnotes.model.Event {
        with(response) {
            if (eventId == 0) throw IllegalArgumentException()
            if (aggregateId.isEmpty()) throw IllegalArgumentException("Event $eventId")

            return when (eventCase!!) {
                Event.GetEventsResponse.EventCase.EVENT_NOT_SET -> throw UnknownEventTypeException(eventId, aggregateId, eventCase.number)
                Event.GetEventsResponse.EventCase.NOTE_CREATED -> NoteCreatedEvent(
                        eventId = eventId,
                        aggId = aggregateId,
                        revision = revision,
                        path = Path.from(noteCreated.path),
                        title = noteCreated.title,
                        content = noteCreated.content
                )
                Event.GetEventsResponse.EventCase.NOTE_DELETED -> NoteDeletedEvent(
                        eventId = eventId,
                        revision = revision,
                        aggId = aggregateId
                )
                Event.GetEventsResponse.EventCase.NOTE_UNDELETED -> NoteUndeletedEvent(
                        eventId = eventId,
                        revision = revision,
                        aggId = aggregateId
                )
                Event.GetEventsResponse.EventCase.ATTACHMENT_ADDED -> AttachmentAddedEvent(
                        eventId = eventId,
                        revision = revision,
                        aggId = aggregateId,
                        name = attachmentAdded.name,
                        content = attachmentAdded.content.toByteArray()
                )
                Event.GetEventsResponse.EventCase.ATTACHMENT_DELETED -> AttachmentDeletedEvent(
                        eventId = eventId,
                        revision = revision,
                        aggId = aggregateId,
                        name = attachmentAdded.name
                )
                Event.GetEventsResponse.EventCase.CONTENT_CHANGED -> ContentChangedEvent(
                        eventId = eventId,
                        revision = revision,
                        aggId = aggregateId,
                        content = contentChanged.content
                )
                Event.GetEventsResponse.EventCase.TITLE_CHANGED -> TitleChangedEvent(
                        eventId = eventId,
                        revision = revision,
                        aggId = aggregateId,
                        title = titleChanged.title
                )
                Event.GetEventsResponse.EventCase.MOVED -> MovedEvent(
                        eventId = eventId,
                        revision = revision,
                        aggId = aggregateId,
                        path = Path.from(moved.path)
                )
                Event.GetEventsResponse.EventCase.FOLDER_CREATED -> FolderCreatedEvent(
                        eventId = eventId,
                        revision = revision,
                        path = Path.from(aggregateId)
                )
                Event.GetEventsResponse.EventCase.FOLDER_DELETED -> FolderDeletedEvent(
                        eventId = eventId,
                        revision = revision,
                        path = Path.from(aggregateId)
                )
            }
        }
    }
}

data class UnknownEventTypeException(val eventId: Int, val aggId: String, val number: Int) : Exception()
