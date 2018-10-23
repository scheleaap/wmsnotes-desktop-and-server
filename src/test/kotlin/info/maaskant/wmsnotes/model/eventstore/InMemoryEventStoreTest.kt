package info.maaskant.wmsnotes.model.eventstore

import info.maaskant.wmsnotes.model.Event

internal class InMemoryEventStoreTest : EventStoreTest() {
    private val eventStore = InMemoryEventStore()

    override fun createInstance(): EventStore {
        return eventStore
    }

    override fun <T : Event> givenAnEvent(eventId: Int, event: T): T {
        return event
    }

}