package info.maaskant.wmsnotes.model.aggregaterepository

import info.maaskant.wmsnotes.model.Aggregate

interface AggregateCache<T : Aggregate<T>> {
    fun get(aggId: String, revision: Int): T?
    fun getLatest(aggId: String, lastRevision: Int? = null): T?
    fun put(note: T)
    fun remove(aggId: String, revision: Int)
}

class NoopAggregateCache<T : Aggregate<T>> : AggregateCache<T> {
    override fun get(aggId: String, revision: Int): T? = null
    override fun getLatest(aggId: String, lastRevision: Int?): T? = null
    override fun put(note: T) {}
    override fun remove(aggId: String, revision: Int) {}
}
