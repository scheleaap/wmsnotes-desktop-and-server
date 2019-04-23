package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.CommandRequest.Companion.randomRequestId
import info.maaskant.wmsnotes.model.note.NoteCommand

data class NoteCommandRequest(
        override val aggId: String,
        override val commands: List<NoteCommand>,
        override val lastRevision: Int? = null,
        override val requestId: Int = randomRequestId()
) : AggregateCommandRequest<NoteCommand>

fun NoteCommandRequest.of(aggId: String, vararg commands: NoteCommand, lastRevision: Int? = null, requestId: Int = randomRequestId()) =
        NoteCommandRequest(aggId, commands.toList(), lastRevision, requestId)
