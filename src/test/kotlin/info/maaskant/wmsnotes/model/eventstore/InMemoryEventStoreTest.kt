package info.maaskant.wmsnotes.model.eventstore

internal class InMemoryEventStoreTest : EventStoreTest() {
    private val eventStore = InMemoryEventStore()

    override fun createInstance(): EventStore {
        return eventStore
    }

}