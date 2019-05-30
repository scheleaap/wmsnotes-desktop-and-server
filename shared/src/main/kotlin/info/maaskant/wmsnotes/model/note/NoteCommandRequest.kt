package info.maaskant.wmsnotes.model.note

import info.maaskant.wmsnotes.model.CommandOrigin
import info.maaskant.wmsnotes.model.CommandOrigin.LOCAL
import info.maaskant.wmsnotes.model.CommandRequest
import info.maaskant.wmsnotes.model.CommandRequest.Companion.randomRequestId

data class NoteCommandRequest(
        override val aggId: String,
        override val commands: List<NoteCommand>,
        override val lastRevision: Int? = null,
        override val requestId: Int = randomRequestId(),
        override val origin: CommandOrigin
) : CommandRequest<NoteCommand> {
    companion object {
        fun of(command: NoteCommand, lastRevision: Int? = null, requestId: Int = randomRequestId(), origin: CommandOrigin = LOCAL) =
                NoteCommandRequest(command.aggId, listOf(command), lastRevision, requestId, origin)
    }
}
