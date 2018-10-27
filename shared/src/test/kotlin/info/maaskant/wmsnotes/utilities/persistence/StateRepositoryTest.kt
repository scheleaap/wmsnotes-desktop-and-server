package info.maaskant.wmsnotes.utilities.persistence

import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal abstract class StateRepositoryTest {
    private val item1 = Item(1)
    private val item2 = Item(2)

    protected val producer: StateProducer<Item> = mockk()

    @Test
    fun `connect and load`() {
        // Given
        givenProducedStates(givenAnItem(item1), givenAnItem(item2))
        val repo = createInstance()

        // When
        repo.connect(producer)
        waitAMoment()
        val state = repo.load()

        // Then
        assertThat(state).isEqualTo(item2)
    }

    @Test
    fun `load, no state present`() {
        // Given
        givenProducedStates()
        val repo = createInstance()

        // When
        repo.connect(producer)
        waitAMoment()
        val state = repo.load()

        // Then
        assertThat(state).isNull()
    }

    protected fun givenProducedStates(vararg items: Item) {
        val observable: Observable<Item> = Observable.fromIterable(items.toList())
        every { producer.getStateUpdates() }.returns(observable)
    }

    protected fun waitAMoment() {
        Thread.sleep(250)
    }

    protected data class Item(val i: Int)

    protected abstract fun createInstance(): StateRepository<Item>
    protected abstract fun givenAnItem(item: Item): Item
}
