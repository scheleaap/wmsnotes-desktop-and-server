package info.maaskant.wmsnotes.utilities.persistence

interface StateRepository<T> {
    fun load(): T?
    fun connect(stateProducer: StateProducer<T>)
}