package info.maaskant.wmsnotes.model.eventstore

internal class InMemoryEventStoreTest : EventStoreTest() {

    override fun createInstance(): EventStore {
        return InMemoryEventStore()
    }

}