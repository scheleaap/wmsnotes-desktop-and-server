package info.maaskant.wmsnotes.model.aggregaterepository

import info.maaskant.wmsnotes.model.Aggregate
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.eventstore.EventStore
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachingAggregateRepository<T : Aggregate<T>> @Inject constructor(
        private val eventStore: EventStore,
        private val aggregateCache: AggregateCache<T>,
        private val emptyAggregate: T
) : AggregateRepository<T> {

    override fun get(aggId: String, revision: Int): T =
            getInternal(aggId = aggId, revision = revision)

    override fun getLatest(aggId: String): T =
            getInternal(aggId = aggId, revision = null)

    private fun getInternal(aggId: String, revision: Int?): T {
        val cached: T? = aggregateCache.getLatest(aggId, lastRevision = revision)
        return eventStore
                .getEventsOfAggregate(aggId, afterRevision = cached?.revision)
                .filter { revision == null || it.revision <= revision }
                .reduceWith({
                    cached ?: emptyAggregate
                }, { note: T, event: Event -> note.apply(event).component1() })
                .blockingGet()
    }

    override fun getAndUpdate(aggId: String): Observable<T> {
        return Observable.defer {
            val cached: T? = aggregateCache.getLatest(aggId, lastRevision = null)
            val current: T = eventStore
                    .getEventsOfAggregate(aggId, afterRevision = cached?.revision)
                    .reduceWith({
                        cached ?: emptyAggregate
                    }, { note: T, event: Event -> note.apply(event).component1() })
                    .blockingGet()
            eventStore
                    .getEventsOfAggregateWithUpdates(aggId, afterRevision = current.revision)
                    .scan(current) { note: T, event: Event -> note.apply(event).component1() }
                    .doOnNext { aggregateCache.put(it) }
        }
    }
}