package info.maaskant.wmsnotes.model

interface CommandExecutor<
        AggregateType : Aggregate<AggregateType>,
        CommandType : Command,
        RequestType : CommandRequest<CommandType>,
        MapperType : CommandToEventMapper<AggregateType>
        > {
    fun canExecuteRequest(request: CommandRequest<*>): RequestType?

    fun execute(request: RequestType): CommandResult
}
