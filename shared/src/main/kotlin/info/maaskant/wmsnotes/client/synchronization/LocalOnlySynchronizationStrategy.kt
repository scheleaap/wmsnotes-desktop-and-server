package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Event

class LocalOnlySynchronizationStrategy : SynchronizationStrategy {
    override fun resolve(localEvents: List<Event>, remoteEvents: List<Event>): SynchronizationStrategy.ResolutionResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}