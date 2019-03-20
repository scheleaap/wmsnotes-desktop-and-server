package info.maaskant.wmsnotes.desktop.client.indexing

import info.maaskant.wmsnotes.desktop.client.indexing.TreeIndex.Change
import info.maaskant.wmsnotes.desktop.client.indexing.TreeIndex.Change.*
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderDeletedEvent
import info.maaskant.wmsnotes.model.folder.FolderEvent
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import info.maaskant.wmsnotes.model.note.NoteDeletedEvent
import info.maaskant.wmsnotes.model.note.NoteUndeletedEvent
import info.maaskant.wmsnotes.model.note.TitleChangedEvent
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TreeIndexTest {
    private val aggId1 = "note-1"
    private val aggId2 = "note-2"
    private val rootPath = Path()
    private val title = "Title"
    private val content = "Text"

    private val scheduler = Schedulers.trampoline()

    private val eventStore: EventStore = mockk()

    private lateinit var treeIndexState: TreeIndexState

    private lateinit var eventUpdatesSubject: PublishSubject<Event>

    @BeforeEach
    fun init() {
        eventUpdatesSubject = PublishSubject.create<Event>()
        treeIndexState = TreeIndexState(isInitialized = false)
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
                NodeAdded(Note(aggId = event2.aggId, parentAggId = null, path = event2.path, title = event2.title))
        ))
    }

    @Test
    fun `note created, twice`() {
        // Given
        val event = noteCreatedEvent(aggId1, rootPath, title)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        eventUpdatesSubject.onNext(event)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(emptyList<Change>())
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
                NodeAdded(Folder(aggId = folderAggId, parentAggId = null, path = path, title = folderTitle)),
                NodeAdded(Note(aggId = aggId1, parentAggId = folderAggId, path = path, title = title))
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
                NodeAdded(Folder(aggId = folderAggId, parentAggId = null, path = path, title = folderTitle)),
                NodeAdded(Note(aggId = aggId1, parentAggId = folderAggId, path = path, title = title))
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
                NodeAdded(Note(aggId = aggId1, parentAggId = null, path = rootPath, title = title)),
                NodeRemoved(event2.aggId),
                NodeAdded(Note(aggId = aggId1, parentAggId = null, path = rootPath, title = title))
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
        val notePath = folder2Path
        val event1 = noteCreatedEvent(aggId1, notePath, title)
        val event2 = noteDeletedEvent(aggId1)
        val event3 = noteUndeletedEvent(aggId1)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        val changeObserver = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)
        val initializationObserver = index.getExistingNodesAsChanges().test()

        // Then
        changeObserver.assertNoErrors()
        assertThat(changeObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title)),
                NodeAdded(Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title)),
                NodeAdded(Note(aggId = aggId1, parentAggId = folder2AggId, path = notePath, title = title)),
                NodeRemoved(aggId1),
                NodeRemoved(folder2AggId),
                NodeRemoved(folder1AggId),
                NodeAdded(Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title)),
                NodeAdded(Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title)),
                NodeAdded(Note(aggId = aggId1, parentAggId = folder2AggId, path = notePath, title = title))
        ))
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title)),
                NodeAdded(Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title)),
                NodeAdded(Note(aggId = aggId1, parentAggId = folder2AggId, path = notePath, title = title))
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
        val initializationObserver = index.getExistingNodesAsChanges().test()

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title)),
                NodeAdded(Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title)),
                NodeRemoved(folder2AggId),
                NodeRemoved(folder1AggId),
                NodeAdded(Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title)),
                NodeAdded(Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title))
        ))
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title)),
                NodeAdded(Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title))
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

    @Test
    fun `delete folder later if it cannot be deleted right away`() {
        // Given
        val path = Path("el1")
        val folderAggId = FolderEvent.aggId(path)
        val event1 = noteCreatedEvent(aggId1, path, title)
        val event2 = folderCreatedEvent(path)
        val event3 = folderDeletedEvent(path)
        val event4 = noteDeletedEvent(aggId1)
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event4)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeRemoved(aggId1),
                NodeRemoved(folderAggId)
        ))
    }

    @Test
    fun `folder created, twice`() {
        // Given
        val event = folderCreatedEvent(Path("el"))
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        eventUpdatesSubject.onNext(event)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(emptyList<Change>())
    }

    @Test
    fun `folder created, empty path`() {
        // Given
        val event = folderCreatedEvent(Path())
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(emptyList<Change>())
    }

    @Test
    fun `title changed`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, rootPath, title)
        val event2 = TitleChangedEvent(eventId = 0, aggId = aggId1, revision = 0, title = "different")
        val index = TreeIndex(eventStore, treeIndexState, scheduler)
        eventUpdatesSubject.onNext(event1)
        val changeObserver = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event2)
        val initializationObserver = index.getExistingNodesAsChanges().test()

        // Then
        changeObserver.assertNoErrors()
        assertThat(changeObserver.values().toList()).isEqualTo(listOf(
                TitleChanged(aggId1, "different")
        ))
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(Note(aggId = aggId1, parentAggId = null, path = rootPath, title = "different"))
        ))
    }

    @Test
    fun initialize() {
        // Given
        val event = noteCreatedEvent(aggId1, rootPath, title)
        every { eventStore.getEvents() }.returns(Observable.just(event))
        val index1 = TreeIndex(eventStore, treeIndexState, scheduler) // Instantiate twice to test double initialization
        val stateObserver = index1.getStateUpdates().test()
        val index2 = TreeIndex(eventStore, stateObserver.values().last(), scheduler)

        // When
        val initializationObserver = index2.getExistingNodesAsChanges().test()

        // Then
        initializationObserver.assertComplete()
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(Note(aggId = aggId1, parentAggId = null, path = rootPath, title = title))
        ))
        verify(exactly = 1) {
            eventStore.getEvents(any())
        }
    }

    @Test
    fun `read state`() {
        // Given
        val event = noteCreatedEvent(aggId1, rootPath, title)
        val index1 = TreeIndex(eventStore, treeIndexState, scheduler) // This instance is supposed to save the state
        val stateObserver = index1.getStateUpdates().test()
        eventUpdatesSubject.onNext(event)
        val index2 = TreeIndex(eventStore, stateObserver.values().last(), scheduler) // This instance is supposed to read the state

        // When
        val initializationObserver = index2.getExistingNodesAsChanges().test()

        // Then
        initializationObserver.assertComplete()
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(Note(aggId = aggId1, parentAggId = null, path = rootPath, title = title))
        ))
    }

    private fun folderCreatedEvent(path: Path) = FolderCreatedEvent(eventId = 0, revision = 1, path = path)
    private fun folderDeletedEvent(path: Path) = FolderDeletedEvent(eventId = 0, revision = 1, path = path)
    private fun noteCreatedEvent(aggId: String, path: Path, title: String) = NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = path, title = title, content = content)
    private fun noteDeletedEvent(aggId: String) = NoteDeletedEvent(eventId = 0, aggId = aggId, revision = 0)
    private fun noteUndeletedEvent(aggId: String) = NoteUndeletedEvent(eventId = 0, aggId = aggId, revision = 0)

}