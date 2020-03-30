package info.maaskant.wmsnotes.model.eventstore

import arrow.core.Either
import info.maaskant.wmsnotes.model.CommandError
import info.maaskant.wmsnotes.model.CommandError.StorageError
import info.maaskant.wmsnotes.model.Event
import io.reactivex.Observable

interface EventStore {
    /**
     * Returns a completing observable that streams all events currently in the repository. Updates are not included.
     *
     * @param afterEventId If specified, only events with an id higher than the parameter will be returned.
     */
    fun getEvents(afterEventId: Int? = null): Observable<Event>

    /**
     * Returns a non-completing observable that streams new events as they are added to the store.
     */
    fun getEventUpdates(): Observable<Event>

    /**
     * Returns a completing observable that streams all events applicable to a given aggregate. Updates are not included.
     *
     * @param aggId The aggregate id.
     * @param afterRevision If specified, only events with a revision higher than the parameter will be returned.
     */
    fun getEventsOfAggregate(aggId: String, afterRevision: Int? = null): Observable<Event>

    /**
     * Returns a non-completing observable that streams all events applicable to a given aggregate, including new events as they are added to the store.
     *
     * @param aggId The aggregate id.
     * @param afterRevision If specified, only events with a revision higher than the parameter will be returned.
     */
    fun getEventsOfAggregateWithUpdates(aggId: String, afterRevision: Int? = null): Observable<Event> {
        return Observable.concat(
                getEventsOfAggregate(aggId, afterRevision),
                getEventUpdates()
                        .filter { it.aggId == aggId }
                        .filter { afterRevision == null || it.revision > afterRevision }
        )
    }

    /** Adds a new event to the repository.
     *
     * @param event The event to add. Its event id must be 0.
     * @return A copy of the event with its event id set.
     * */
    fun appendEvent(event: Event): Either<StorageError, Event>
}
