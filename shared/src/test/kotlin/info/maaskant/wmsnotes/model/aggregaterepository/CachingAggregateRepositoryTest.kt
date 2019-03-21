package info.maaskant.wmsnotes.model.aggregaterepository

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.note.AttachmentAddedEvent
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import io.mockk.*
import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CachingAggregateRepositoryTest {
    private val aggId = "note"

    private val event1 = NoteCreatedEvent(eventId = 1, aggId = aggId, revision = 1, path = Path("path"), title = "Title", content = "Text")
    private val event2 = AttachmentAddedEvent(eventId = 2, aggId = aggId, revision = 2, name = "att-1", content = "data1".toByteArray())
    private val event3 = AttachmentAddedEvent(eventId = 3, aggId = aggId, revision = 3, name = "att-2", content = "data2".toByteArray())

    private val emptyNote = Note()
    private val noteAfterEvent1 = emptyNote.apply(event1).component1()
    private val noteAfterEvent2 = noteAfterEvent1.apply(event2).component1()
    private val noteAfterEvent3 = noteAfterEvent2.apply(event3).component1()

    private val wrappedRepository: AggregateRepository<Note> = mockk()
    private val aggregateCache: AggregateCache<Note> = mockk()
    private val eventStore: EventStore = mockk()

    @BeforeEach
    fun init() {
        clearMocks(
                wrappedRepository,
                eventStore,
                aggregateCache
        )
        every { aggregateCache.put(any()) } just Runs
    }

    @Test
    fun `get, not present in cache`() {
        // Given
        every { wrappedRepository.get(aggId, noteAfterEvent2.revision) }.returns(noteAfterEvent2)
        every { aggregateCache.get(aggId, noteAfterEvent1.revision) }.returns(null)
        every { aggregateCache.get(aggId, noteAfterEvent2.revision) }.returns(null)
        every { aggregateCache.getLatest(aggId, noteAfterEvent2.revision) }.returns(null)
        every { eventStore.getEventsOfAggregate(aggId, afterRevision = null) }.returns(Observable.just(event1, event2, event3))
        val p = CachingAggregateRepository(eventStore, aggregateCache, emptyNote)

        // When
        val note = p.get(aggId, noteAfterEvent2.revision)

        // Then
        assertThat(note).isEqualTo(noteAfterEvent2)
    }

    @Test
    fun `get, old version present in cache`() {
        // Given
        every { wrappedRepository.get(aggId, noteAfterEvent2.revision) }.throws(Exception())
        every { aggregateCache.get(aggId, noteAfterEvent1.revision) }.returns(noteAfterEvent1)
        every { aggregateCache.get(aggId, noteAfterEvent2.revision) }.returns(null)
        every { aggregateCache.getLatest(aggId, noteAfterEvent2.revision) }.returns(noteAfterEvent1)
        every { eventStore.getEventsOfAggregate(aggId, afterRevision = noteAfterEvent1.revision) }.returns(Observable.just(event2, event3))
        val p = CachingAggregateRepository(eventStore, aggregateCache, emptyNote)

        // When
        val note = p.get(aggId, noteAfterEvent2.revision)

        // Then
        assertThat(note).isEqualTo(noteAfterEvent2)
    }

    @Test
    fun `get, too new version present in cache`() {
        // Given
        every { wrappedRepository.get(aggId, noteAfterEvent2.revision) }.throws(Exception())
        every { aggregateCache.get(aggId, noteAfterEvent1.revision) }.returns(null)
        every { aggregateCache.get(aggId, noteAfterEvent2.revision) }.returns(null)
        every { aggregateCache.get(aggId, noteAfterEvent3.revision) }.returns(noteAfterEvent3)
        every { aggregateCache.getLatest(aggId, lastRevision = null) }.returns(noteAfterEvent3)
        every { aggregateCache.getLatest(aggId, lastRevision = noteAfterEvent2.revision) }.returns(noteAfterEvent2)
        every { eventStore.getEventsOfAggregate(aggId, afterRevision = noteAfterEvent2.revision) }.returns(Observable.just(event3))
        every { eventStore.getEventsOfAggregate(aggId, afterRevision = noteAfterEvent3.revision) }.returns(Observable.empty())
        val p = CachingAggregateRepository(eventStore, aggregateCache, emptyNote)

        // When
        val note = p.get(aggId, noteAfterEvent2.revision)

        // Then
        assertThat(note).isEqualTo(noteAfterEvent2)
    }

    @Test
    fun `get with updates, nothing present in cache`() {
        // Given
        val events = createHotObservable()
        every { aggregateCache.getLatest(aggId, lastRevision = null) }.returns(null)
        every { eventStore.getEventsOfAggregate(aggId, afterRevision = null) }.returns(Observable.just(event1, event2))
        every { eventStore.getEventsOfAggregateWithUpdates(aggId, afterRevision = noteAfterEvent2.revision) }.returns(events)
        val p = CachingAggregateRepository(eventStore, aggregateCache, emptyNote)

        // When
        val observer = p.getAndUpdate(aggId).test()
        events.onNext(event3)

        // Then
        observer.assertNotComplete()
        observer.assertNotTerminated()
        assertThat(observer.values()).isEqualTo(listOf(noteAfterEvent2, noteAfterEvent3))
        verify {
            aggregateCache.put(noteAfterEvent2)
            aggregateCache.put(noteAfterEvent3)
        }
    }

    @Test
    fun `get with updates, old version present in cache`() {
        // Given
        val events = createHotObservable()
        every { aggregateCache.getLatest(aggId, lastRevision = null) }.returns(noteAfterEvent1)
        every { eventStore.getEventsOfAggregate(aggId, afterRevision = noteAfterEvent1.revision) }.returns(Observable.just(event2))
        every { eventStore.getEventsOfAggregateWithUpdates(aggId, afterRevision = noteAfterEvent2.revision) }.returns(events)
        val p = CachingAggregateRepository(eventStore, aggregateCache, emptyNote)

        // When
        val observer = p.getAndUpdate(aggId).test()
        events.onNext(event3)

        // Then
        observer.assertNotComplete()
        observer.assertNotTerminated()
        assertThat(observer.values()).isEqualTo(listOf(noteAfterEvent2, noteAfterEvent3))
    }

    @Test
    fun `get with updates, latest version present in cache`() {
        // Given
        val events = createHotObservable()
        every { aggregateCache.getLatest(aggId, lastRevision = null) }.returns(noteAfterEvent2)
        every { eventStore.getEventsOfAggregate(aggId, afterRevision = noteAfterEvent2.revision) }.returns(Observable.empty())
        every { eventStore.getEventsOfAggregateWithUpdates(aggId, afterRevision = noteAfterEvent2.revision) }.returns(events)
        val p = CachingAggregateRepository(eventStore, aggregateCache, emptyNote)

        // When
        val observer = p.getAndUpdate(aggId).test()
        events.onNext(event3)

        // Then
        observer.assertNotComplete()
        observer.assertNotTerminated()
        assertThat(observer.values()).isEqualTo(listOf(noteAfterEvent2, noteAfterEvent3))
    }

    private fun createHotObservable(vararg events: Event): Subject<Event> {
        val subject = ReplaySubject.create<Event>()
        for (event in events) {
            subject.onNext(event)
        }
        return subject
    }

}