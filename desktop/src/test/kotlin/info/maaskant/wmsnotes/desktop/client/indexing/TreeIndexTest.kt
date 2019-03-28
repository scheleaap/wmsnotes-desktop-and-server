package info.maaskant.wmsnotes.desktop.client.indexing

import info.maaskant.wmsnotes.desktop.client.indexing.TreeIndex.Change
import info.maaskant.wmsnotes.desktop.client.indexing.TreeIndex.Change.*
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.folder.Folder.Companion.aggId
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderDeletedEvent
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

    private val sortingStrategy: Comparator<Node> = mockk()

    private lateinit var treeIndexState: TreeIndexState

    private lateinit var eventUpdatesSubject: PublishSubject<Event>

    @BeforeEach
    fun init() {
        eventUpdatesSubject = PublishSubject.create<Event>()
        treeIndexState = TreeIndexState(isInitialized = false)
        clearMocks(
                eventStore,
                sortingStrategy
        )
        every { eventStore.getEvents() }.returns(Observable.empty())
        every { eventStore.getEventUpdates() }.returns(eventUpdatesSubject as Observable<Event>)
        every { sortingStrategy.compare(any(), any()) }.returns(0)
    }

    @Test
    fun `getChanges should only return new events`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, rootPath, title)
        val event2 = noteCreatedEvent(aggId2, rootPath, title)
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)

        // When
        eventUpdatesSubject.onNext(event1)
        val observer = index.getChanges().test()
        eventUpdatesSubject.onNext(event2)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Note(aggId = event2.aggId, parentAggId = null, path = event2.path, title = event2.title), folderIndex = 1)
        ))
    }

    @Test
    fun `note created, twice`() {
        // Given
        val event = noteCreatedEvent(aggId1, rootPath, title)
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
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
        val folderAggId = aggId(path)
        val event1 = noteCreatedEvent(aggId1, path, title)
        val event2 = folderCreatedEvent(path)
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(aggId = folderAggId, parentAggId = null, path = path, title = folderTitle), folderIndex = 0),
                NodeAdded(Note(aggId = aggId1, parentAggId = folderAggId, path = path, title = title), folderIndex = 0)
        ))
    }

    @Test
    fun `note created after folder`() {
        // Given
        val folderTitle = "el"
        val path = Path(folderTitle)
        val folderAggId = aggId(path)
        val event1 = folderCreatedEvent(path)
        val event2 = noteCreatedEvent(aggId1, path, title)
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(aggId = folderAggId, parentAggId = null, path = path, title = folderTitle), folderIndex = 0),
                NodeAdded(Note(aggId = aggId1, parentAggId = folderAggId, path = path, title = title), folderIndex = 0)
        ))
    }

    @Test
    fun `do not create and delete folders for root notes`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, rootPath, title)
        val event2 = noteDeletedEvent(aggId1)
        val event3 = noteUndeletedEvent(aggId1)
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Note(aggId = aggId1, parentAggId = null, path = rootPath, title = title), folderIndex = 0),
                NodeRemoved(event2.aggId),
                NodeAdded(Note(aggId = aggId1, parentAggId = null, path = rootPath, title = title), folderIndex = 0)
        ))
    }

    @Test
    fun `note deleted, note does not exist`() {
        // Given
        val event = noteDeletedEvent(aggId1)
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
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
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
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
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
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
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
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
        val folder1AggId = aggId(folder1Path)
        val folder2AggId = aggId(folder2Path)
        val event1 = noteCreatedEvent(aggId1, folder2Path, title)
        val event2 = noteDeletedEvent(aggId1)
        val event3 = noteUndeletedEvent(aggId1)
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
        val changeObserver = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)
        val initializationObserver = index.getExistingNodesAsChanges().test()

        // Then
        changeObserver.assertNoErrors()
        assertThat(changeObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title), folderIndex = 0),
                NodeAdded(Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title), folderIndex = 0),
                NodeAdded(Note(aggId = aggId1, parentAggId = folder2AggId, path = folder2Path, title = title), folderIndex = 0),
                NodeRemoved(aggId1),
                NodeRemoved(folder2AggId),
                NodeRemoved(folder1AggId),
                NodeAdded(Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title), folderIndex = 0),
                NodeAdded(Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title), folderIndex = 0),
                NodeAdded(Note(aggId = aggId1, parentAggId = folder2AggId, path = folder2Path, title = title), folderIndex = 0)
        ))
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title), folderIndex = 0),
                NodeAdded(Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title), folderIndex = 0),
                NodeAdded(Note(aggId = aggId1, parentAggId = folder2AggId, path = folder2Path, title = title), folderIndex = 0)
        ))
    }

    @Test
    fun `automatically add and remove parent folders when creating, deleting and creating a folder`() {
        // Given
        val folder1Title = "el1"
        val folder2Title = "el2"
        val folder1Path = Path(folder1Title)
        val folder2Path = Path(folder1Title, folder2Title)
        val folder1AggId = aggId(folder1Path)
        val folder2AggId = aggId(folder2Path)
        val event1 = folderCreatedEvent(folder2Path)
        val event2 = folderDeletedEvent(folder2Path)
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
        val observer = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event1)
        val initializationObserver = index.getExistingNodesAsChanges().test()

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title), folderIndex = 0),
                NodeAdded(Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title), folderIndex = 0),
                NodeRemoved(folder2AggId),
                NodeRemoved(folder1AggId),
                NodeAdded(Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title), folderIndex = 0),
                NodeAdded(Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title), folderIndex = 0)
        ))
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title), folderIndex = 0),
                NodeAdded(Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title), folderIndex = 0)
        ))
    }

    @Test
    fun `do not remove a folder when something is removed and it still contains a note afterwards`() {
        // Given
        val path = Path("el")
        val event1 = noteCreatedEvent(aggId1, path, title)
        val event2 = noteCreatedEvent(aggId2, path, title)
        val event3 = noteDeletedEvent(aggId1)
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
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
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
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
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
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
        val folder2AggId = aggId(folder2Path)
        val event1 = folderCreatedEvent(folder2Path)
        val event2 = folderCreatedEvent(folder1Path)
        val event3 = folderDeletedEvent(folder2Path)
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
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
        val folderAggId = aggId(path)
        val event1 = noteCreatedEvent(aggId1, path, title)
        val event2 = folderCreatedEvent(path)
        val event3 = folderDeletedEvent(path)
        val event4 = noteDeletedEvent(aggId1)
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
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
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
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
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
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
        val folderTitle = "el"
        val path = Path(folderTitle)
        val folderAggId = aggId(path)
        val event1 = folderCreatedEvent(path)
        val event2 = noteCreatedEvent(aggId1, path, "Title 1")
        val event3 = noteCreatedEvent(aggId2, path, "Title 2")
        val event4 = TitleChangedEvent(eventId = 0, aggId = aggId2, revision = 0, title = "Title 0")
        val node2 = Note(aggId = aggId1, parentAggId = folderAggId, path = path, title = "Title 1")
        val node3a = Note(aggId = aggId2, parentAggId = folderAggId, path = path, title = "Title 2")
        val node3b = Note(aggId = aggId2, parentAggId = folderAggId, path = path, title = "Title 0")
        every { sortingStrategy.compare(node2, node3a) }.returns(-1)
        every { sortingStrategy.compare(node3a, node2) }.returns(1)
        every { sortingStrategy.compare(node2, node3b) }.returns(1)
        every { sortingStrategy.compare(node3b, node2) }.returns(-1)
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)
        val changeObserver = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event4)
        val initializationObserver = index.getExistingNodesAsChanges().test()

        // Then
        changeObserver.assertNoErrors()
        assertThat(changeObserver.values().toList()).isEqualTo(listOf(
                TitleChanged(aggId2, "Title 0", 1, 0)
        ))
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(aggId = folderAggId, parentAggId = null, path = path, title = folderTitle), folderIndex = 0),
                NodeAdded(node3b, folderIndex = 0),
                NodeAdded(node2, folderIndex = 1)
        ))
    }

    @Test
    fun `folder index, if all nodes are equal`() {
        // Given
        val folder1Title = "el1"
        val folder2Title = "el1.1"
        val folder3Title = "el2"
        val folder1Path = Path(folder1Title)
        val folder2Path = Path(folder1Title, folder2Title)
        val folder3Path = Path(folder3Title)
        val folder1AggId = aggId(folder1Path)
        val folder2AggId = aggId(folder2Path)
        val folder3AggId = aggId(folder3Path)
        val event1 = folderCreatedEvent(folder1Path)
        val event2 = noteCreatedEvent(aggId1, folder1Path, title)
        val event3 = folderCreatedEvent(folder2Path)
        val event4 = folderCreatedEvent(folder3Path)
        val node1 = Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title)
        val node2 = Note(aggId = aggId1, parentAggId = folder1AggId, path = folder1Path, title = title)
        val node3 = Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title)
        val node4 = Folder(aggId = folder3AggId, parentAggId = null, path = folder3Path, title = folder3Title)
        every { sortingStrategy.compare(any(), any()) }.returns(0)
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
        val changeObserver = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)
        eventUpdatesSubject.onNext(event4)
        val initializationObserver = index.getExistingNodesAsChanges().test()

        // Then
        changeObserver.assertNoErrors()
        assertThat(changeObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(node1, folderIndex = 0),
                NodeAdded(node2, folderIndex = 0),
                NodeAdded(node3, folderIndex = 1),
                NodeAdded(node4, folderIndex = 1)
        ))
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(node1, folderIndex = 0),
                NodeAdded(node4, folderIndex = 1), // Because breadth-first
                NodeAdded(node2, folderIndex = 0),
                NodeAdded(node3, folderIndex = 1)
        ))
    }

    @Test
    fun `folder index, use sorting strategy`() {
        // Given
        val folder1Title = "el1"
        val folder2Title = "el1.1"
        val folder3Title = "el2"
        val folder1Path = Path(folder1Title)
        val folder2Path = Path(folder1Title, folder2Title)
        val folder3Path = Path(folder3Title)
        val folder1AggId = aggId(folder1Path)
        val folder2AggId = aggId(folder2Path)
        val folder3AggId = aggId(folder3Path)
        val event1 = folderCreatedEvent(folder1Path)
        val event2 = noteCreatedEvent(aggId1, folder1Path, title)
        val event3 = folderCreatedEvent(folder2Path)
        val event4 = folderCreatedEvent(folder3Path)
        val node1 = Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title)
        val node2 = Note(aggId = aggId1, parentAggId = folder1AggId, path = folder1Path, title = title)
        val node3 = Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title)
        val node4 = Folder(aggId = folder3AggId, parentAggId = null, path = folder3Path, title = folder3Title)
        every { sortingStrategy.compare(node1, node4) }.returns(1)
        every { sortingStrategy.compare(node4, node1) }.returns(-1)
        every { sortingStrategy.compare(node2, node3) }.returns(1)
        every { sortingStrategy.compare(node3, node2) }.returns(-1)
        val index = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
        val changeObserver = index.getChanges().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)
        eventUpdatesSubject.onNext(event4)
        val initializationObserver = index.getExistingNodesAsChanges().test()

        // Then
        changeObserver.assertNoErrors()
        assertThat(changeObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(node1, folderIndex = 0),
                NodeAdded(node2, folderIndex = 0),
                NodeAdded(node3, folderIndex = 0),
                NodeAdded(node4, folderIndex = 0)
        ))
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(node4, folderIndex = 0),
                NodeAdded(node1, folderIndex = 1), // Because breadth-first
                NodeAdded(node3, folderIndex = 0),
                NodeAdded(node2, folderIndex = 1)
        ))
    }

    @Test
    fun initialize() {
        // Given
        val event = noteCreatedEvent(aggId1, rootPath, title)
        every { eventStore.getEvents() }.returns(Observable.just(event))
        val index1 = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler) // Instantiate twice to test double initialization
        val stateObserver = index1.getStateUpdates().test()
        val index2 = TreeIndex(eventStore, sortingStrategy, stateObserver.values().last(), scheduler)

        // When
        val initializationObserver = index2.getExistingNodesAsChanges().test()

        // Then
        initializationObserver.assertComplete()
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(Note(aggId = aggId1, parentAggId = null, path = rootPath, title = title), folderIndex = 0)
        ))
        verify(exactly = 1) {
            eventStore.getEvents(any())
        }
    }

    @Test
    fun `read state`() {
        // Given
        val event = noteCreatedEvent(aggId1, rootPath, title)
        val index1 = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler) // This instance is supposed to save the state
        val stateObserver = index1.getStateUpdates().test()
        eventUpdatesSubject.onNext(event)
        val index2 = TreeIndex(eventStore, sortingStrategy, stateObserver.values().last(), scheduler) // This instance is supposed to read the state

        // When
        val initializationObserver = index2.getExistingNodesAsChanges().test()

        // Then
        initializationObserver.assertComplete()
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(Note(aggId = aggId1, parentAggId = null, path = rootPath, title = title), folderIndex = 0)
        ))
    }

    private fun folderCreatedEvent(path: Path) = FolderCreatedEvent(eventId = 0, revision = 1, path = path)
    private fun folderDeletedEvent(path: Path) = FolderDeletedEvent(eventId = 0, revision = 1, path = path)
    private fun noteCreatedEvent(aggId: String, path: Path, title: String) = NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = path, title = title, content = content)
    private fun noteDeletedEvent(aggId: String) = NoteDeletedEvent(eventId = 0, aggId = aggId, revision = 0)
    private fun noteUndeletedEvent(aggId: String) = NoteUndeletedEvent(eventId = 0, aggId = aggId, revision = 0)

}