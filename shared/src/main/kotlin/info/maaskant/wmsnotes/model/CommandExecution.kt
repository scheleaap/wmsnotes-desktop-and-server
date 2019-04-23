package info.maaskant.wmsnotes.model

import io.reactivex.subjects.SingleSubject
import java.util.concurrent.TimeUnit

class CommandExecution {
    companion object {
        fun executeBlocking(commandBus: CommandBus, commandRequest: CommandRequest, timeoutDuration: Long, timeoutUnit: TimeUnit): CommandResult {
            val subject: SingleSubject<CommandResult> = SingleSubject.create<CommandResult>()
            commandBus.results
                    .filter { it.requestId == commandRequest.requestId }
                    .firstOrError()
                    .subscribe(subject)
            commandBus.requests.onNext(commandRequest)
            return subject
                    .timeout(timeoutDuration, timeoutUnit)
                    .blockingGet()
        }
    }
}