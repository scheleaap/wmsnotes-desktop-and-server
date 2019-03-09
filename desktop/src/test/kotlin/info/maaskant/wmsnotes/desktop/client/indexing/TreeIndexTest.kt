package info.maaskant.wmsnotes.desktop.client.indexing

import info.maaskant.wmsnotes.desktop.client.indexing.TreeIndex.Change
import info.maaskant.wmsnotes.desktop.client.indexing.TreeIndex.Change.NodeAdded
import info.maaskant.wmsnotes.desktop.client.indexing.TreeIndex.Change.NodeRemoved
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderEvent
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import info.maaskant.wmsnotes.model.note.NoteDeletedEvent
import info.maaskant.wmsnotes.model.note.NoteUndeletedEvent
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*

internal class TreeIndexTest {
    private val aggId1 = UUID.randomUUID().toString()
    private val aggId2 = UUID.randomUUID().toString()
    private val rootPath = Path()
    private val path = Path("el1", "el2")
    private val title = "Title"
    private val content = "Text"

    private val scheduler = Schedulers.trampoline()

    private val eventStore: EventStore = mockk()

    private lateinit var treeIndexState: TreeIndexState

    private lateinit var eventUpdatesSubject: PublishSubject<Event>

    @BeforeEach
    fun init() {
        eventUpdatesSubject = PublishSubject.create<Event>()
        treeIndexState = TreeIndexState(/*isInitialized = false*/)
        clearMocks(
                eventStore
        )
        every { eventStore.getEvents() }.returns(Observable.empty())
        every { eventStore.getEventUpdates() }.returns(eventUpdatesSubject as Observable<Event>)
    }

    @Test
    fun `folder created by itself`() {
        // Given
        val event = folderCreatedEvent(path)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(event.aggId, event.path, event.path.elements.last()))
        ))
    }

    @Test
    fun `getChanges should only return new events`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, path, title)
        val event2 = noteCreatedEvent(aggId2, path, title)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)

        // When
        eventUpdatesSubject.onNext(event1)
        val observer = index.getChanges().test()
        eventUpdatesSubject.onNext(event2)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Note(event2.aggId, event2.path, event2.title))
        ))
    }

    @Test
    fun `note created, in root`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, rootPath, title)
        val event2 = noteCreatedEvent(aggId2, rootPath, title)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Note(event1.aggId, event1.path, event1.title)),
                NodeAdded(Note(event2.aggId, event2.path, event2.title))
        ))
    }

    @Test
    fun `note created before folder`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, path, title)
        val event2 = folderCreatedEvent(path)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(FolderEvent.aggId(path), path, path.elements.last())),
                NodeAdded(Note(event1.aggId, event1.path, event1.title))
        ))
    }

    @Test
    fun `note created after folder`() {
        // Given
        val event1 = folderCreatedEvent(path)
        val event2 = noteCreatedEvent(aggId1, path, title)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(event1.aggId, event1.path, event1.path.elements.last())),
                NodeAdded(Note(event2.aggId, event2.path, event2.title))
        ))
    }

    @Test
    fun `note deleted, in root`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, rootPath, title)
        val event2 = noteDeletedEvent(aggId1)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        eventUpdatesSubject.onNext(event1)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event2)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeRemoved(event2.aggId)
        ))
    }

    @Test
    fun `note deleted, note does not exist`() {
        // Given
        val event = noteDeletedEvent(aggId1)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(emptyList<Change>())
    }

    @Test
    fun `note deleted, twice`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, rootPath, title)
        val event2 = noteDeletedEvent(aggId1)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event2)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(emptyList<Change>())
    }

    @Test
    fun `note undeleted, in root`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, rootPath, title)
        val event2 = noteDeletedEvent(aggId1)
        val event3 = noteUndeletedEvent(aggId1)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event3)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Note(event1.aggId, event1.path, event1.title))
        ))
    }

    @Test
    fun `note undeleted, note does not exist`() {
        // Given
        val event = noteUndeletedEvent(aggId1)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(emptyList<Change>())
    }

    @Test
    fun `note undeleted, twice`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, rootPath, title)
        val event2 = noteDeletedEvent(aggId1)
        val event3 = noteUndeletedEvent(aggId1  )
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event3)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(emptyList<Change>())
    }

    @Test
    fun `note deleted before folder`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, path, title)
        val event2 = noteDeletedEvent(aggId1)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        eventUpdatesSubject.onNext(event1)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event2)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeRemoved(event2.aggId),
                NodeRemoved(FolderEvent.aggId(event1.path))
        ))
    }


    // TODO:
    // - note move
    // - folder create, duplicate
    // - folder create, empty path = error
    // - delete note before folder
    // - delete note after folder
    // - undelete before folder
    // - undelete after folder

    private fun folderCreatedEvent(path: Path): FolderCreatedEvent = FolderCreatedEvent(eventId = 0, revision = 1, path = path)
    private fun noteCreatedEvent(aggId: String, path: Path, title: String) = NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = path, title = title, content = content)
    private fun noteDeletedEvent(aggId: String) = NoteDeletedEvent(eventId = 0, aggId = aggId, revision = 0)
    private fun noteUndeletedEvent(aggId: String) = NoteUndeletedEvent(eventId = 0, aggId = aggId, revision = 0)

}