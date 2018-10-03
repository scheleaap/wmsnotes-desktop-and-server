package info.maaskant.wmsnotes.server.api

import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import info.maaskant.wmsnotes.server.command.grpc.Command
import info.maaskant.wmsnotes.server.command.grpc.Event

// Server:
// model event -> API event response
// API command request -> model command

// Client
// API event response -> model event
// model event -> API command request
// model event -> model command

class GrpcConverters {

    companion object {

        fun toModelClass(response: Event.GetEventsResponse): info.maaskant.wmsnotes.model.Event {
            with(response) {
                if (eventId == 0) throw IllegalArgumentException()
                if (noteId.isEmpty()) throw IllegalArgumentException("Event $eventId")

                return when (eventCase!!) {
                    Event.GetEventsResponse.EventCase.NOTE_CREATED -> NoteCreatedEvent(
                            eventId = eventId,
                            noteId = noteId,
                            title = noteCreated.title
                    )
                    Event.GetEventsResponse.EventCase.NOTE_DELETED -> NoteDeletedEvent(
                            eventId = eventId,
                            noteId = noteId
                    )
                    Event.GetEventsResponse.EventCase.EVENT_NOT_SET -> throw IllegalArgumentException("Event $eventId")
                }
            }
        }

        fun toGrpcGetEventsResponse(event: info.maaskant.wmsnotes.model.Event): Event.GetEventsResponse {
            with(event) {
                val builder = Event.GetEventsResponse.newBuilder()
                        .setEventId(eventId)
                        .setNoteId(noteId)

                when (this) {
                    is info.maaskant.wmsnotes.model.NoteCreatedEvent -> {
                        builder.noteCreatedBuilder.setTitle(title).build()
                    }
                    is NoteDeletedEvent -> {
                        builder.noteDeletedBuilder.build()
                    }
                }
                return builder.build()
            }
        }

        fun toGrpcPostCommandRequest(event: info.maaskant.wmsnotes.model.Event, lastEventId: Int?): Command.PostCommandRequest {
            with(event) {
                val builder = Command.PostCommandRequest.newBuilder()
                        .setNoteId(noteId)
                if (lastEventId != null) {
                    builder.setLastEventId(lastEventId)
                }
                when (this) {
                    is info.maaskant.wmsnotes.model.NoteCreatedEvent -> {
                        builder.createNoteBuilder.setTitle(title).build()
                    }
                    is NoteDeletedEvent -> {
                        builder.deleteNoteBuilder.build()
                    }
                }
                return builder.build()
            }
        }
    }

}