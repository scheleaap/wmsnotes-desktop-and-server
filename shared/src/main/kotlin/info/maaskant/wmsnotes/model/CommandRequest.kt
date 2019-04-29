package info.maaskant.wmsnotes.model

import kotlin.random.Random

/** A request to sequentially execute commands all related to one aggregate. */
interface CommandRequest<out CommandType : Command> {
    /** An identifier to trace the request. */
    val requestId: Int

    /** The id of the aggregate. */
    val aggId: String

    /** The commands to execute. All commands must refer to the same aggregate. */
    val commands: List< CommandType>

    /** The last known revision of the aggregate. Used for optimistic locking. */
    val lastRevision: Int?

    companion object {
        fun randomRequestId() = Random.Default.nextInt()
    }
}