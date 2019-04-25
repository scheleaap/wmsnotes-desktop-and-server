package info.maaskant.wmsnotes.model

import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class CommandBus {
    val requests: Subject<CommandRequest> = PublishSubject.create<CommandRequest>().toSerialized()
    val results: Subject<CommandResult> = PublishSubject.create<CommandResult>().toSerialized()
}
