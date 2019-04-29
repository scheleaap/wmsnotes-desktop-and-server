package info.maaskant.wmsnotes.model

import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class CommandBus {
    val requests: Subject<CommandRequest<Command>> = PublishSubject.create<CommandRequest<Command>>().toSerialized()
    val results: Subject<CommandResult> = PublishSubject.create<CommandResult>().toSerialized()
}
