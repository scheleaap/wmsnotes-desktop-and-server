package info.maaskant.wmsnotes.server.api

import info.maaskant.wmsnotes.model.AttachmentAddedEvent
import info.maaskant.wmsnotes.model.AttachmentDeletedEvent
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import info.maaskant.wmsnotes.server.command.grpc.Command
import info.maaskant.wmsnotes.server.command.grpc.Event
import javax.inject.Inject

// TODO: Add command request -> model command <implemented in server>
class GrpcCommandMapper @Inject constructor() {

    // Replace with model Command -> PostCommandRequest mapper + model Event to model Command mapper?
    fun toGrpcPostCommandRequest(event: info.maaskant.wmsnotes.model.Event, lastRevision: Int?): Command.PostCommandRequest {
        val builder = Command.PostCommandRequest.newBuilder()
                .setNoteId(event.noteId)
        if (lastRevision != null) {
            builder.setLastRevision(lastRevision)
        }
        when (event) {
            is info.maaskant.wmsnotes.model.NoteCreatedEvent -> {
                builder.createNoteBuilder.setTitle(event.title).build()
            }
            is NoteDeletedEvent -> {
                builder.deleteNoteBuilder.build()
            }
            is AttachmentAddedEvent -> TODO()
            is AttachmentDeletedEvent -> TODO()
        }
        return builder.build()
    }

}