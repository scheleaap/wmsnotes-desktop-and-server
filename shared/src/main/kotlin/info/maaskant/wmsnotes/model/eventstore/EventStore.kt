package info.maaskant.wmsnotes.model.eventstore

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
     * Returns a completing observable that streams all events applicable to a given note. Updates are not included.
     *
     * @param noteId The note id.
     * @param afterRevision If specified, only events with a revision higher than the parameter will be returned.
     */
    fun getEventsOfNote(noteId: String, afterRevision: Int? = null): Observable<Event>

    /**
     * Returns a non-completing observable that streams all events applicable to a given note, including new events as they are added to the store.
     *
     * @param noteId The note id.
     * @param afterRevision If specified, only events with a revision higher than the parameter will be returned.
     */
    fun getEventsOfNoteWithUpdates(noteId: String, afterRevision: Int? = null): Observable<Event> {
        return Observable.concat(
                getEventsOfNote(noteId, afterRevision),
                getEventUpdates()
                        .filter { it.noteId == noteId }
                        .filter { afterRevision == null || it.revision > afterRevision }
        )
    }

    /** Adds a new event to the repository.
     *
     * @param event The event to add. Its event id must be 0.
     * @return A copy of the event with its event id set.
     * */
    fun appendEvent(event: Event): Event
}
