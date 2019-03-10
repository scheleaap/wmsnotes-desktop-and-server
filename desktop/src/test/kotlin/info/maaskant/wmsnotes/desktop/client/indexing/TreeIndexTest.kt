package info.maaskant.wmsnotes.desktop.client.indexing

import info.maaskant.wmsnotes.desktop.client.indexing.TreeIndex.Change
import info.maaskant.wmsnotes.desktop.client.indexing.TreeIndex.Change.NodeAdded
import info.maaskant.wmsnotes.desktop.client.indexing.TreeIndex.Change.NodeRemoved
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderDeletedEvent
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
    fun `getChanges should only return new events`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, rootPath, title)
        val event2 = noteCreatedEvent(aggId2, rootPath, title)
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
    fun `note created before folder`() {
        // Given
        val folderTitle = "el"
        val path = Path(folderTitle)
        val folderAggId = FolderEvent.aggId(path)
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
                NodeAdded(Folder(folderAggId, path, folderTitle)),
                NodeAdded(Note(aggId1, path, title))
        ))
    }

    @Test
    fun `note created after folder`() {
        // Given
        val folderTitle = "el"
        val path = Path(folderTitle)
        val folderAggId = FolderEvent.aggId(path)
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
                NodeAdded(Folder(folderAggId, path, folderTitle)),
                NodeAdded(Note(aggId1, path, title))
        ))
    }

    @Test
    fun `do not create and delete folders for root notes`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, rootPath, title)
        val event2 = noteDeletedEvent(aggId1)
        val event3 = noteUndeletedEvent(aggId1)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Note(aggId1, rootPath, title)),
                NodeRemoved(event2.aggId),
                NodeAdded(Note(aggId1, rootPath, title))
        ))
    }

    @Disabled
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

    @Disabled
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

    @Disabled
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

    @Disabled
    @Test
    fun `note undeleted, twice`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, rootPath, title)
        val event2 = noteDeletedEvent(aggId1)
        val event3 = noteUndeletedEvent(aggId1)
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
    fun `automatically add and remove parent folders when creating, deleting and undeleting a note`() {
        // Given
        val folder1Title = "el1"
        val folder2Title = "el2"
        val folder1Path = Path(folder1Title)
        val folder2Path = Path(folder1Title, folder2Title)
        val folder1AggId = FolderEvent.aggId(folder1Path)
        val folder2AggId = FolderEvent.aggId(folder2Path)
        val notePath =folder2Path
        val event1 = noteCreatedEvent(aggId1, notePath, title)
        val event2 = noteDeletedEvent(aggId1)
        val event3 = noteUndeletedEvent(aggId1)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(folder1AggId, folder1Path, folder1Title)),
                NodeAdded(Folder(folder2AggId, folder2Path, folder2Title)),
                NodeAdded(Note(aggId1, notePath, title)),
                NodeRemoved(aggId1),
                NodeRemoved(folder2AggId),
                NodeRemoved(folder1AggId),
                NodeAdded(Folder(folder1AggId, folder1Path, folder1Title)),
                NodeAdded(Folder(folder2AggId, folder2Path, folder2Title)),
                NodeAdded(Note(aggId1, notePath, title))
        ))
    }

    @Test
    fun `automatically add and remove parent folders when creating, deleting and creating a folder`() {
        // Given
        val folder1Title = "el1"
        val folder2Title = "el2"
        val folder1Path = Path(folder1Title)
        val folder2Path = Path(folder1Title, folder2Title)
        val folder1AggId = FolderEvent.aggId(folder1Path)
        val folder2AggId = FolderEvent.aggId(folder2Path)
        val event1 = folderCreatedEvent(folder2Path)
        val event2 = folderDeletedEvent(folder2Path)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event1)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(folder1AggId, folder1Path, folder1Title)),
                NodeAdded(Folder(folder2AggId, folder2Path, folder2Title)),
                NodeRemoved(folder2AggId),
                NodeRemoved(folder1AggId),
                NodeAdded(Folder(folder1AggId, folder1Path, folder1Title)),
                NodeAdded(Folder(folder2AggId, folder2Path, folder2Title))
        ))
    }

    @Test
    fun `do not remove a folder when something is removed and it still contains a note afterwards`() {
        // Given
        val path = Path("el")
        val event1 = noteCreatedEvent(aggId1, path, title)
        val event2 = noteCreatedEvent(aggId2, path, title)
        val event3 = noteDeletedEvent(aggId1)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event3)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeRemoved(aggId1)
        ))
    }

    @Test
    fun `do not remove a folder when something is removed and it still contains another folder afterwards`() {
        // Given
        val path = Path("el")
        val event1 = noteCreatedEvent(aggId1, path, title)
        val event2 = folderCreatedEvent(path)
        val event3 = noteDeletedEvent(aggId1)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event3)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeRemoved(aggId1)
        ))
    }

    @Test
    fun `only delete automatically created folders when a note is deleted`() {
        // Given
        val path = Path("el1")
        val event1 = noteCreatedEvent(aggId1, path, title)
        val event2 = folderCreatedEvent(path)
        val event3 = noteDeletedEvent(aggId1)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event3)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeRemoved(aggId1)
        ))
    }

    @Test
    fun `only delete automatically created folders when another folder is deleted`() {
        // Given
        val folder1Path = Path("el1")
        val folder2Path = Path("el1", "el2")
        val folder2AggId = FolderEvent.aggId(folder2Path)
        val event1 = folderCreatedEvent(folder2Path)
        val event2 = folderCreatedEvent(folder1Path)
        val event3 = folderDeletedEvent(folder2Path)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event3)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeRemoved(folder2AggId)
        ))
    }

    // TODO:
    // - note move
    // - folder create, duplicate
    // - folder create, empty path = error
    // - create folder, create note, delete folder, delete note -> check that folder is deleted

    private fun folderCreatedEvent(path: Path) = FolderCreatedEvent(eventId = 0, revision = 1, path = path)
    private fun folderDeletedEvent(path: Path) = FolderDeletedEvent(eventId = 0, revision = 1, path = path)
    private fun noteCreatedEvent(aggId: String, path: Path, title: String) = NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = path, title = title, content = content)
    private fun noteDeletedEvent(aggId: String) = NoteDeletedEvent(eventId = 0, aggId = aggId, revision = 0)
    private fun noteUndeletedEvent(aggId: String) = NoteUndeletedEvent(eventId = 0, aggId = aggId, revision = 0)

}