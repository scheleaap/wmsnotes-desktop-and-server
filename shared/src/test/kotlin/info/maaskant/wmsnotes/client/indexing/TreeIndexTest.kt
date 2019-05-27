package info.maaskant.wmsnotes.client.indexing

import info.maaskant.wmsnotes.client.indexing.TreeIndexEvent.*
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.folder.Folder.Companion.aggId
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderDeletedEvent
import info.maaskant.wmsnotes.model.note.*
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
    private val aggId3 = "note-3"
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
    fun `getEvents should only return new events`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, rootPath, title)
        val event2 = noteCreatedEvent(aggId2, rootPath, title)
        val index = createInstanceAndStart()

        // When
        eventUpdatesSubject.onNext(event1)
        val observer = index.getEvents().test()
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
        val index = createInstanceAndStart()
        eventUpdatesSubject.onNext(event)
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(emptyList<TreeIndexEvent>())
    }

    @Test
    fun `note created before folder`() {
        // Given
        val folderTitle = "el"
        val folderPath = Path(folderTitle)
        val folderAggId = aggId(folderPath)
        val event1 = noteCreatedEvent(aggId1, folderPath, title)
        val event2 = folderCreatedEvent(folderPath)
        val index = createInstanceAndStart()
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(aggId = folderAggId, parentAggId = null, path = folderPath, title = folderTitle), folderIndex = 0),
                NodeAdded(Note(aggId = aggId1, parentAggId = folderAggId, path = folderPath, title = title), folderIndex = 0)
        ))
    }

    @Test
    fun `note created after folder`() {
        // Given
        val folderTitle = "el"
        val folderPath = Path(folderTitle)
        val folderAggId = aggId(folderPath)
        val event1 = folderCreatedEvent(folderPath)
        val event2 = noteCreatedEvent(aggId1, folderPath, title)
        val index = createInstanceAndStart()
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(Folder(aggId = folderAggId, parentAggId = null, path = folderPath, title = folderTitle), folderIndex = 0),
                NodeAdded(Note(aggId = aggId1, parentAggId = folderAggId, path = folderPath, title = title), folderIndex = 0)
        ))
    }

    @Test
    fun `do not create and delete folders for root notes`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, rootPath, title)
        val event2 = noteDeletedEvent(aggId1)
        val event3 = noteUndeletedEvent(aggId1)
        val node = Note(aggId = aggId1, parentAggId = null, path = rootPath, title = title)
        val index = createInstanceAndStart()
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(node, folderIndex = 0),
                NodeRemoved(node),
                NodeAdded(node, folderIndex = 0)
        ))
    }

    @Test
    fun `note deleted, note does not exist`() {
        // Given
        val event = noteDeletedEvent(aggId1)
        val index = createInstanceAndStart()
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(emptyList<TreeIndexEvent>())
    }

    @Test
    fun `note deleted, twice`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, rootPath, title)
        val event2 = noteDeletedEvent(aggId1)
        val index = createInstanceAndStart()
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event2)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(emptyList<TreeIndexEvent>())
    }

    @Test
    fun `note undeleted, note does not exist`() {
        // Given
        val event = noteUndeletedEvent(aggId1)
        val index = createInstanceAndStart()
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(emptyList<TreeIndexEvent>())
    }

    @Test
    fun `note undeleted, twice`() {
        // Given
        val event1 = noteCreatedEvent(aggId1, rootPath, title)
        val event2 = noteDeletedEvent(aggId1)
        val event3 = noteUndeletedEvent(aggId1)
        val index = createInstanceAndStart()
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event3)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(emptyList<TreeIndexEvent>())
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
        val node1 = Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title)
        val node2 = Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title)
        val node3 = Note(aggId = aggId1, parentAggId = folder2AggId, path = folder2Path, title = title)
        val index = createInstanceAndStart()
        val eventObserver = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)
        val initializationObserver = index.getNodes().test()

        // Then
        eventObserver.assertNoErrors()
        assertThat(eventObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(node1, folderIndex = 0),
                NodeAdded(node2, folderIndex = 0),
                NodeAdded(node3, folderIndex = 0),
                NodeRemoved(node3),
                NodeRemoved(node2),
                NodeRemoved(node1),
                NodeAdded(node1, folderIndex = 0),
                NodeAdded(node2, folderIndex = 0),
                NodeAdded(node3, folderIndex = 0)
        ))
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                IndexedValue(0, node1),
                IndexedValue(0, node2),
                IndexedValue(0, node3)
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
        val node1 = Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title)
        val node2 = Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title)
        val index = createInstanceAndStart()
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event1)
        val initializationObserver = index.getNodes().test()

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeAdded(node1, folderIndex = 0),
                NodeAdded(node2, folderIndex = 0),
                NodeRemoved(node2),
                NodeRemoved(node1),
                NodeAdded(node1, folderIndex = 0),
                NodeAdded(node2, folderIndex = 0)
        ))
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                IndexedValue(0, node1),
                IndexedValue(0, node2)
        ))
    }

    @Test
    fun `do not remove a folder when something is removed and it still contains a note afterwards`() {
        // Given
        val folderTitle = "el"
        val folderPath = Path(folderTitle)
        val folderAggId = aggId(folderPath)
        val event1 = noteCreatedEvent(aggId1, folderPath, title)
        val event2 = noteCreatedEvent(aggId2, folderPath, title)
        val event3 = noteDeletedEvent(aggId1)
        val index = createInstanceAndStart()
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event3)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeRemoved(Note(aggId = aggId1, parentAggId = folderAggId, path = folderPath, title = title))
        ))
    }

    @Test
    fun `do not remove a folder when something is removed and it still contains another folder afterwards`() {
        // Given
        val folderTitle = "el"
        val folderPath = Path(folderTitle)
        val folderAggId = aggId(folderPath)
        val event1 = noteCreatedEvent(aggId1, folderPath, title)
        val event2 = folderCreatedEvent(folderPath)
        val event3 = noteDeletedEvent(aggId1)
        val index = createInstanceAndStart()
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event3)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeRemoved(Note(aggId = aggId1, parentAggId = folderAggId, path = folderPath, title = title))
        ))
    }

    @Test
    fun `only delete automatically created folders when a note is deleted`() {
        // Given
        val folderTitle = "el"
        val folderPath = Path(folderTitle)
        val folderAggId = aggId(folderPath)
        val event1 = noteCreatedEvent(aggId1, folderPath, title)
        val event2 = folderCreatedEvent(folderPath)
        val event3 = noteDeletedEvent(aggId1)
        val index = createInstanceAndStart()
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event3)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeRemoved(Note(aggId = aggId1, parentAggId = folderAggId, path = folderPath, title = title))
        ))
    }

    @Test
    fun `only delete automatically created folders when another folder is deleted`() {
        // Given
        val folder1Title = "el1"
        val folder2Title = "el2"
        val folder1Path = Path(folder1Title)
        val folder2Path = Path(folder1Title, folder2Title)
        val folder1AggId = aggId(folder1Path)
        val folder2AggId = aggId(folder2Path)
        val event1 = folderCreatedEvent(folder2Path)
        val event2 = folderCreatedEvent(folder1Path)
        val event3 = folderDeletedEvent(folder2Path)
        val index = createInstanceAndStart()
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event3)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeRemoved(Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title))
        ))
    }

    @Test
    fun `delete folder later if it cannot be deleted right away`() {
        // Given
        val folderTitle = "el"
        val folderPath = Path(folderTitle)
        val folderAggId = aggId(folderPath)
        val event1 = noteCreatedEvent(aggId1, folderPath, title)
        val event2 = folderCreatedEvent(folderPath)
        val event3 = folderDeletedEvent(folderPath)
        val event4 = noteDeletedEvent(aggId1)
        val index = createInstanceAndStart()
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event4)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(listOf(
                NodeRemoved(Note(aggId = aggId1, parentAggId = folderAggId, path = folderPath, title = title)),
                NodeRemoved(Folder(aggId = folderAggId, parentAggId = null, path = folderPath, title = folderTitle))
        ))
    }

    @Test
    fun `folder created, twice`() {
        // Given
        val event = folderCreatedEvent(Path("el"))
        val index = createInstanceAndStart()
        eventUpdatesSubject.onNext(event)
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(emptyList<TreeIndexEvent>())
    }

    @Test
    fun `folder created, empty path`() {
        // Given
        val event = folderCreatedEvent(Path())
        val index = createInstanceAndStart()
        val observer = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event)

        // Then
        observer.assertNoErrors()
        assertThat(observer.values().toList()).isEqualTo(emptyList<TreeIndexEvent>())
    }

    @Test
    fun `title changed`() {
        // Given
        val folderTitle = "el"
        val folderPath = Path(folderTitle)
        val folderAggId = aggId(folderPath)
        val event1 = folderCreatedEvent(folderPath)
        val event2 = noteCreatedEvent(aggId1, folderPath, "Title 1")
        val event3 = noteCreatedEvent(aggId2, folderPath, "Title 2")
        val event4 = TitleChangedEvent(eventId = 0, aggId = aggId2, revision = 0, title = "Title 0")
        val node2 = Note(aggId = aggId1, parentAggId = folderAggId, path = folderPath, title = "Title 1")
        val node3a = Note(aggId = aggId2, parentAggId = folderAggId, path = folderPath, title = "Title 2")
        val node3b = Note(aggId = aggId2, parentAggId = folderAggId, path = folderPath, title = "Title 0")
        every { sortingStrategy.compare(node2, node3a) }.returns(-1)
        every { sortingStrategy.compare(node3a, node2) }.returns(1)
        every { sortingStrategy.compare(node2, node3b) }.returns(1)
        every { sortingStrategy.compare(node3b, node2) }.returns(-1)
        val index = createInstanceAndStart()
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)
        val eventObserver = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event4)
        val initializationObserver = index.getNodes().test()

        // Then
        eventObserver.assertNoErrors()
        assertThat(eventObserver.values().toList()).isEqualTo(listOf(
                TitleChanged(node3b, 1, 0)
        ))
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                IndexedValue(0, Folder(aggId = folderAggId, parentAggId = null, path = folderPath, title = folderTitle)),
                IndexedValue(0, node3b),
                IndexedValue(1, node2)
        ))
    }

    @Test
    fun moved() {
        // Given
        val folder1Title = "el1"
        val folder2Title = "el2"
        val folder1Path = Path(folder1Title)
        val folder2Path = Path(folder2Title)
        val folder1AggId = aggId(folder1Path)
        val folder2AggId = aggId(folder2Path)
        val event1 = folderCreatedEvent(folder1Path)
        val event2 = folderCreatedEvent(folder2Path)
        val event3 = noteCreatedEvent(aggId1, folder1Path, "Title 1")
        val event4 = noteCreatedEvent(aggId2, folder2Path, "Title 2")
        val event5 = noteCreatedEvent(aggId3, folder1Path, "Title 3")
        val event6 = MovedEvent(eventId = 0, aggId = aggId3, revision = 0, path = folder2Path)
        val node3 = Note(aggId = aggId1, parentAggId = folder1AggId, path = folder1Path, title = "Title 1")
        val node4 = Note(aggId = aggId2, parentAggId = folder2AggId, path = folder2Path, title = "Title 2")
        val node5a = Note(aggId = aggId3, parentAggId = folder1AggId, path = folder1Path, title = "Title 3")
        val node5b = Note(aggId = aggId3, parentAggId = folder2AggId, path = folder2Path, title = "Title 3")
        every { sortingStrategy.compare(node3, node5a) }.returns(-1)
        every { sortingStrategy.compare(node5a, node3) }.returns(1)
        every { sortingStrategy.compare(node4, node5b) }.returns(-1)
        every { sortingStrategy.compare(node5b, node4) }.returns(1)
        val index = createInstanceAndStart()
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)
        eventUpdatesSubject.onNext(event4)
        eventUpdatesSubject.onNext(event5)
        val eventObserver = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event6)
        val initializationObserver = index.getNodes().test()

        // Then
        eventObserver.assertNoErrors()
        assertThat(eventObserver.values().toList()).isEqualTo(listOf(
                NodeRemoved(node5a),
                NodeAdded(node5b, 1)
        ))
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                IndexedValue(0, Folder(aggId = folder1AggId, parentAggId = null, path = folder1Path, title = folder1Title)),
                IndexedValue(1, Folder(aggId = folder2AggId, parentAggId = null, path = folder2Path, title = folder2Title)),
                IndexedValue(0, node3),
                IndexedValue(0, node4),
                IndexedValue(1, node5b)
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
        val index = createInstanceAndStart()
        val eventObserver = index.getEvents().test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)
        eventUpdatesSubject.onNext(event4)
        val initializationObserver = index.getNodes().test()

        // Then
        eventObserver.assertNoErrors()
        assertThat(eventObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(node1, folderIndex = 0),
                NodeAdded(node2, folderIndex = 0),
                NodeAdded(node3, folderIndex = 1),
                NodeAdded(node4, folderIndex = 1)
        ))
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                IndexedValue(0, node1),
                IndexedValue(1, node4), // Because breadth-first
                IndexedValue(0, node2),
                IndexedValue(1, node3)
        ))
    }

    @Test
    fun `filter by folder`() {
        // Given
        val folder1Title = "el1"
        val folder2Title = "el1.1"
        val folder1Path = Path(folder1Title)
        val folder2Path = Path(folder1Title, folder2Title)
        val folder1AggId = aggId(folder1Path)
        val folder2AggId = aggId(folder2Path)
        val event1 = folderCreatedEvent(folder1Path)
        val event2 = noteCreatedEvent(aggId1, folder1Path, title)
        val event3 = folderCreatedEvent(folder2Path)
        val node2 = Note(aggId = aggId1, parentAggId = folder1AggId, path = folder1Path, title = title)
        val node3 = Folder(aggId = folder2AggId, parentAggId = folder1AggId, path = folder2Path, title = folder2Title)
        val index = createInstanceAndStart()
        val eventObserver = index.getEvents(filterByFolder = folder1Path).test()

        // When
        eventUpdatesSubject.onNext(event1)
        eventUpdatesSubject.onNext(event2)
        eventUpdatesSubject.onNext(event3)
        val initializationObserver = index.getNodes(filterByFolder = folder1Path).test()

        // Then
        eventObserver.assertNoErrors()
        assertThat(eventObserver.values().toList()).isEqualTo(listOf(
                NodeAdded(node2, folderIndex = 0),
                NodeAdded(node3, folderIndex = 1)
        ))
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                IndexedValue(0, node2),
                IndexedValue(1, node3)
        ))
    }

    @Test
    fun `initialize from getEvents()`() {
        // Given
        val event = noteCreatedEvent(aggId1, rootPath, title)
        every { eventStore.getEvents() }.returns(Observable.just(event))
        val index1 = createInstanceAndStart() // Instantiate twice to test double initialization
        val stateObserver = index1.getStateUpdates().test()
        val index2 = TreeIndex(eventStore, sortingStrategy, stateObserver.values().last(), scheduler)

        // When
        val initializationObserver = index2.getNodes().test()

        // Then
        initializationObserver.assertComplete()
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                IndexedValue(0, Note(aggId = aggId1, parentAggId = null, path = rootPath, title = title))
        ))
        verify(exactly = 1) {
            eventStore.getEvents(any())
        }
    }

    @Test
    fun `initialize from state`() {
        // Given
        val event = noteCreatedEvent(aggId1, rootPath, title)
        val index1 = createInstanceAndStart() // This instance is supposed to save the state
        val stateObserver = index1.getStateUpdates().test()
        eventUpdatesSubject.onNext(event)
        val index2 = TreeIndex(eventStore, sortingStrategy, stateObserver.values().last(), scheduler) // This instance is supposed to read the state

        // When
        val initializationObserver = index2.getNodes().test()

        // Then
        initializationObserver.assertComplete()
        initializationObserver.assertNoErrors()
        assertThat(initializationObserver.values().toList()).isEqualTo(listOf(
                IndexedValue(0, Note(aggId = aggId1, parentAggId = null, path = rootPath, title = title))
        ))
    }

    private fun createInstanceAndStart(): TreeIndex = TreeIndex(eventStore, sortingStrategy, treeIndexState, scheduler)
            .also { it.start() }

    private fun folderCreatedEvent(path: Path) = FolderCreatedEvent(eventId = 0, revision = 1, path = path)
    private fun folderDeletedEvent(path: Path) = FolderDeletedEvent(eventId = 0, revision = 1, path = path)
    private fun noteCreatedEvent(aggId: String, path: Path, title: String) = NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = path, title = title, content = content)
    private fun noteDeletedEvent(aggId: String) = NoteDeletedEvent(eventId = 0, aggId = aggId, revision = 0)
    private fun noteUndeletedEvent(aggId: String) = NoteUndeletedEvent(eventId = 0, aggId = aggId, revision = 0)

}