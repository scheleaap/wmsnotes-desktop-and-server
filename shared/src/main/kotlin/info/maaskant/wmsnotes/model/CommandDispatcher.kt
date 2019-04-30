//package info.maaskant.wmsnotes.model
//
//import com.sun.prism.PhongMaterial
//import info.maaskant.wmsnotes.model.eventstore.EventStore
//import info.maaskant.wmsnotes.utilities.logger
//import io.reactivex.schedulers.Schedulers
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class CommandDispatcher @Inject constructor(
//        private val bus: CommandBus,
//        private val eventStore: EventStore,
//        private vararg val executors: CommandExecutor<*, *, *, *>
//) {
//
//    private val logger by logger()
//
//    init { --> connect()
//        bus.requests
//                .observeOn(Schedulers.io())
//                .doOnNext { logger.debug("Received command: $it") }
//                .map(this::dispatchToExecutor)
//                .subscribeBy(onNext, onError, bus.results)
//    }
//
//    private fun <T:Command> dispatchToExecutor(commandRequest: CommandRequest<T>): CommandResult {
//        for (commandExecutor in executors) {
//            val commandExecutor1: CommandExecutor<*, *, *, *> = commandExecutor
//            val result = dispatchIfPossible(commandExecutor1, commandRequest)
////            if (request is Handled) {
////                return request.newEvent
////            }
//        }
//        throw IllegalArgumentException("Command $commandRequest cannot be handled by any known command executor")
//    }
//
//    private fun <
//            AggregateType : Aggregate<AggregateType>,
//            CommandType : Command,
//            RequestType : CommandRequest<CommandType>,
//            MapperType : CommandToEventMapper<AggregateType>
//            >
//            dispatchIfPossible(commandExecutor: CommandExecutor<AggregateType, CommandType, RequestType, MapperType>, commandRequest: RequestType): CommandResult? {
//        val request2: RequestType? = commandExecutor.canExecuteRequest(commandRequest)
//        return if (request2 != null) {
//            commandExecutor.execute(request2)
//        } else {
//            null
//        }
//    }
//}