package info.maaskant.wmsnotes.model

import io.reactivex.rxkotlin.toObservable

interface Aggregate<T : Aggregate<T>> {
    val revision: Int
    val aggId: String

    fun apply(event: Event): Pair<T, Event?>

    companion object {
        fun <T : Aggregate<T>> apply(base: T, events: List<Event>): T =
                events.toObservable()
                        .reduceWith({ base }, { aggregate: T, event: Event -> aggregate.apply(event).component1() })
                        .blockingGet()
    }
}
