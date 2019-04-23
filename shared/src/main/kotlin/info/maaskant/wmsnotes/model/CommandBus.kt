package info.maaskant.wmsnotes.model

import io.reactivex.subjects.Subject

data class CommandBus(
        val requests: Subject<CommandRequest>,
        val results: Subject<CommandResult>
)
