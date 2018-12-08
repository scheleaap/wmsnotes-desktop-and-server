package info.maaskant.wmsnotes.client.synchronization


class CompensatingActionExecutor {
    fun execute(compensatingAction: CompensatingAction): ExecutionResult {
        TODO()
    }

    data class ExecutionResult(
            val success: Boolean,
            val newLocalEvents: List<Event>,
            val newRemoteEvents: List<Event>
    )
}
