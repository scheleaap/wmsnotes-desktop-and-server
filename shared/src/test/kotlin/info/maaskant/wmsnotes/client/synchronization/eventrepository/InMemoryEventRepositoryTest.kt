package info.maaskant.wmsnotes.client.synchronization.eventrepository

internal class InMemoryEventRepositoryTest : EventRepositoryTest() {

    override fun createInstance(): ModifiableEventRepository {
        return InMemoryEventRepository()
    }

}