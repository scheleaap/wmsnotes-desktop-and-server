package info.maaskant.wmsnotes.model

import kotlin.random.Random

interface CommandRequest {
    /** An identifier to trace the request. */
    val requestId: Int

    companion object {
        fun randomRequestId() = Random.Default.nextInt()
    }
}
