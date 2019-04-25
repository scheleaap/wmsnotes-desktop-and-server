package info.maaskant.wmsnotes.model.note

import info.maaskant.wmsnotes.model.AggregateCommandRequest
import info.maaskant.wmsnotes.model.CommandRequest.Companion.randomRequestId

data class NoteCommandRequest(
        override val aggId: String,
        override val commands: List<NoteCommand>,
        override val lastRevision: Int? = null,
        override val requestId: Int = randomRequestId()
) : AggregateCommandRequest<NoteCommand>

fun NoteCommandRequest.of(aggId: String, vararg commands: NoteCommand, lastRevision: Int? = null, requestId: Int = randomRequestId()) =
        NoteCommandRequest(aggId, commands.toList(), lastRevision, requestId)
