package info.maaskant.wmsnotes.server.api

import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import info.maaskant.wmsnotes.server.command.grpc.Event

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

        fun toGrpcClass(event: info.maaskant.wmsnotes.model.Event): Event.GetEventsResponse {
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
    }

}