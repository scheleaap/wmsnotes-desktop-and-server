package info.maaskant.wmsnotes.utilities.persistence

import io.reactivex.Observable

interface StateProducer<T> {
    /**
     * Returns an observable that always emits the latest update to the state (if any) as well as any further updates.
     *
     * Implementations typically back the [Observable] with a [io.reactivex.subjects.BehaviorSubject].
     */
    fun getStateUpdates(): Observable<T>
}
