package info.maaskant.wmsnotes.client.synchronization.eventrepository

internal class InMemoryModifiableEventRepositoryTest : ModifiableEventRepositoryTest() {

    override fun createInstance(): ModifiableEventRepository {
        return InMemoryModifiableEventRepository()
    }

}