package info.maaskant.wmsnotes.model

/** A request to sequentially execute commands all related to one aggregate. */
interface AggregateCommandRequest<CommandType : AggregateCommand> : CommandRequest {
    /** The id of the aggregate. */
    val aggId: String

    /** The commands to execute. All commands must refer to the same aggregate. */
    val commands: List<CommandType>

    /** The last known revision of the aggregate. Used for optimistic locking. */
    val lastRevision: Int?
}