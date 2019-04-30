package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.SingleTransformer
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.SingleSubject
import java.util.concurrent.TimeUnit

class CommandExecution {
    companion object {
        private val logger by logger()

        fun executeBlocking(commandBus: CommandBus, commandRequest: CommandRequest<Command>, timeout: Duration?): CommandResult {
            val subject: SingleSubject<CommandResult> = SingleSubject.create<CommandResult>()
            commandBus.results
                    .filter { it.requestId == commandRequest.requestId }
                    .firstOrError()
                    .subscribeBy(
                            onSuccess = subject::onSuccess,
                            onError = { logger.warn("Error", it) }
                    )
            commandBus.requests.onNext(commandRequest)
            return subject
                    .compose(withTimeout(timeout))
                    .blockingGet()
        }

        private fun withTimeout(duration: Duration?): SingleTransformer<CommandResult, CommandResult> {
            return SingleTransformer { it2 ->
                if (duration != null) {
                    it2.timeout(duration.duration, duration.unit)
                } else {
                    it2
                }
            }
        }
    }

    data class Duration(val duration: Long, val unit: TimeUnit)
}