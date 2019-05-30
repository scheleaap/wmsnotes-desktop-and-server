package info.maaskant.wmsnotes.model.folder

import info.maaskant.wmsnotes.model.CommandOrigin
import info.maaskant.wmsnotes.model.CommandOrigin.LOCAL
import info.maaskant.wmsnotes.model.CommandRequest
import info.maaskant.wmsnotes.model.CommandRequest.Companion.randomRequestId

data class FolderCommandRequest(
        override val aggId: String,
        override val commands: List<FolderCommand>,
        override val lastRevision: Int? = null,
        override val requestId: Int = randomRequestId(),
        override val origin: CommandOrigin
) : CommandRequest<FolderCommand> {
    companion object {
        fun of(command: FolderCommand, lastRevision: Int? = null, requestId: Int = randomRequestId(), origin: CommandOrigin = LOCAL) =
                FolderCommandRequest(command.aggId, listOf(command), lastRevision, requestId, origin)
    }
}
