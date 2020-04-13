package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.CommandError

data class SynchronizationResult(
        val errors: List<Pair<String, CommandError>>
)
