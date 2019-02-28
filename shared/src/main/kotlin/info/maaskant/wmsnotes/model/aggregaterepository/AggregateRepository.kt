package info.maaskant.wmsnotes.model.aggregaterepository

import info.maaskant.wmsnotes.model.Aggregate
import io.reactivex.Observable

interface AggregateRepository<T : Aggregate<T>> {
    fun get(aggId: String, revision: Int): T
    fun getAndUpdate(aggId: String): Observable<T>
}
