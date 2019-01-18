package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.synchronization.CompensatingActionExecutor.EventIdAndRevision
import info.maaskant.wmsnotes.client.synchronization.CompensatingActionExecutor.ExecutionResult
import info.maaskant.wmsnotes.client.synchronization.SynchronizationStrategy.ResolutionResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.SynchronizationStrategy.ResolutionResult.Solution
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import io.mockk.*
import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class NewSynchronizerTest {

    private val localEvents: ModifiableEventRepository = mockk()
    private val remoteEvents: ModifiableEventRepository = mockk()
    private val synchronizationStrategy: SynchronizationStrategy = mockk()
    private val compensatingActionExecutor: CompensatingActionExecutor = mockk()
    private lateinit var initialState: SynchronizerState

    @BeforeEach
    fun init() {
        clearMocks(
                localEvents,
                remoteEvents,
                synchronizationStrategy
        )
        every { localEvents.removeEvent(any()) }.just(Runs)
        every { remoteEvents.removeEvent(any()) }.just(Runs)
        initialState = SynchronizerState.create()
    }

    @Test
    fun `nothing happens if there are no local or remote events`() {
        // Given
        every { localEvents.getEvents() }.returns(Observable.empty())
        every { remoteEvents.getEvents() }.returns(Observable.empty())
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verify {
            synchronizationStrategy.resolve(any(), any()).wasNot(Called)
            compensatingActionExecutor.execute(any()).wasNot(Called)
        }
    }

    @Test
    fun `nothing happens if synchronization strategy has no solution`() {
        // Given
        val localOutboundEvent: Event = modelEvent(eventId = 11, noteId = 1, revision = 1)
        every { localEvents.getEvents() }.returns(Observable.just(localOutboundEvent))
        every { remoteEvents.getEvents() }.returns(Observable.empty())
        every { synchronizationStrategy.resolve(any(), any()) }.returns(NoSolution)
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verify {
            synchronizationStrategy.resolve(listOf(localOutboundEvent), emptyList())
            compensatingActionExecutor.execute(any()).wasNot(Called)
            // TODO
//            localEvents.removeEvent(any()).wasNot(Called)
//            remoteEvents.removeEvent(any()).wasNot(Called)
        }
    }

    @Test
    fun `one note, one compensating action`() {
        // Given
        val localEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val localEvent2 = modelEvent(eventId = 12, noteId = 1, revision = 2)
        val remoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val remoteEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 12)
        val localEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val localEventForRemoteEvent2 = modelEvent(eventId = 14, noteId = 1, revision = 4)
        val remoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val remoteEventForLocalEvent2 = modelEvent(eventId = 4, noteId = 1, revision = 14)
        every { localEvents.getEvents() }.returns(Observable.just(localEvent1, localEvent2))
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteEvent1, remoteEvent2))
        val compensatingAction = CompensatingAction(
                compensatedLocalEvents = listOf(localEvent1, localEvent2),
                compensatedRemoteEvents = listOf(remoteEvent1, remoteEvent2),
                newLocalEvents = listOf(localEventForRemoteEvent1, localEventForRemoteEvent2),
                newRemoteEvents = listOf(remoteEventForLocalEvent1, remoteEventForLocalEvent2)
        )
        every {
            synchronizationStrategy.resolve(
                    localEvents = listOf(localEvent1, localEvent2),
                    remoteEvents = listOf(remoteEvent1, remoteEvent2)
            )
        }.returns(Solution(compensatingAction))
        every { compensatingActionExecutor.execute(compensatingAction) }.returns(ExecutionResult(
                success = true,
                newLocalEvents = listOf(EventIdAndRevision(localEventForRemoteEvent1), EventIdAndRevision(localEventForRemoteEvent2)),
                newRemoteEvents = listOf(EventIdAndRevision(remoteEventForLocalEvent1), EventIdAndRevision(remoteEventForLocalEvent2))
        ))
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            compensatingActionExecutor.execute(compensatingAction)
            remoteEvents.removeEvent(remoteEvent1)
            remoteEvents.removeEvent(remoteEvent2)
            localEvents.removeEvent(localEvent1)
            localEvents.removeEvent(localEvent2)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[localEvent1.noteId]).isEqualTo(localEventForRemoteEvent2.revision)
        assertThat(finalState.lastKnownRemoteRevisions[localEvent1.noteId]).isEqualTo(remoteEventForLocalEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[localEvent1.noteId]).isEqualTo(localEventForRemoteEvent2.revision)
    }

    @Test
    fun `one note, multiple compensating actions`() {
        // Given
        val localEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val localEvent2 = modelEvent(eventId = 12, noteId = 1, revision = 2)
        val remoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val remoteEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 12)
        val localEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val localEventForRemoteEvent2 = modelEvent(eventId = 14, noteId = 1, revision = 4)
        val remoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val remoteEventForLocalEvent2 = modelEvent(eventId = 4, noteId = 1, revision = 14)
        every { localEvents.getEvents() }.returns(Observable.just(localEvent1, localEvent2))
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteEvent1, remoteEvent2))
        val compensatingAction1 = CompensatingAction(
                compensatedLocalEvents = listOf(localEvent1),
                compensatedRemoteEvents = listOf(remoteEvent1),
                newLocalEvents = listOf(localEventForRemoteEvent1),
                newRemoteEvents = listOf(remoteEventForLocalEvent1)
        )
        val compensatingAction2 = CompensatingAction(
                compensatedLocalEvents = listOf(localEvent2),
                compensatedRemoteEvents = listOf(remoteEvent2),
                newLocalEvents = listOf(localEventForRemoteEvent2),
                newRemoteEvents = listOf(remoteEventForLocalEvent2)
        )
        every {
            synchronizationStrategy.resolve(
                    localEvents = listOf(localEvent1, localEvent2),
                    remoteEvents = listOf(remoteEvent1, remoteEvent2)
            )
        }.returns(Solution(listOf(compensatingAction1, compensatingAction2)))
        every { compensatingActionExecutor.execute(compensatingAction1) }.returns(ExecutionResult(
                success = true,
                newLocalEvents = listOf(EventIdAndRevision(localEventForRemoteEvent1)),
                newRemoteEvents = listOf(EventIdAndRevision(remoteEventForLocalEvent1))
        ))
        every { compensatingActionExecutor.execute(compensatingAction2) }.returns(ExecutionResult(
                success = true,
                newLocalEvents = listOf(EventIdAndRevision(localEventForRemoteEvent2)),
                newRemoteEvents = listOf(EventIdAndRevision(remoteEventForLocalEvent2))
        ))
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            compensatingActionExecutor.execute(compensatingAction1)
            remoteEvents.removeEvent(remoteEvent1)
            localEvents.removeEvent(localEvent1)
            compensatingActionExecutor.execute(compensatingAction2)
            remoteEvents.removeEvent(remoteEvent2)
            localEvents.removeEvent(localEvent2)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[localEvent1.noteId]).isEqualTo(localEventForRemoteEvent2.revision)
        assertThat(finalState.lastKnownRemoteRevisions[localEvent1.noteId]).isEqualTo(remoteEventForLocalEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[localEvent1.noteId]).isEqualTo(localEventForRemoteEvent2.revision)
    }

    @Test
    fun `one note, multiple compensating actions, executing one action fails`() {
        // Given
        val localEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val localEvent2 = modelEvent(eventId = 12, noteId = 1, revision = 2)
        val remoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val remoteEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 12)
        val localEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val localEventForRemoteEvent2 = modelEvent(eventId = 14, noteId = 1, revision = 4)
        val remoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val remoteEventForLocalEvent2 = modelEvent(eventId = 4, noteId = 1, revision = 14)
        every { localEvents.getEvents() }.returns(Observable.just(localEvent1, localEvent2))
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteEvent1, remoteEvent2))
        val compensatingAction1 = CompensatingAction(
                compensatedLocalEvents = listOf(localEvent1),
                compensatedRemoteEvents = listOf(remoteEvent1),
                newLocalEvents = listOf(localEventForRemoteEvent1),
                newRemoteEvents = listOf(remoteEventForLocalEvent1)
        )
        val compensatingAction2 = CompensatingAction(
                compensatedLocalEvents = listOf(localEvent2),
                compensatedRemoteEvents = listOf(remoteEvent2),
                newLocalEvents = listOf(localEventForRemoteEvent2),
                newRemoteEvents = listOf(remoteEventForLocalEvent2)
        )
        every {
            synchronizationStrategy.resolve(
                    localEvents = listOf(localEvent1, localEvent2),
                    remoteEvents = listOf(remoteEvent1, remoteEvent2)
            )
        }.returns(Solution(listOf(compensatingAction1, compensatingAction2)))
        every { compensatingActionExecutor.execute(compensatingAction1) }.returns(ExecutionResult(
                success = false,
                newLocalEvents = emptyList(), // Local failed
                newRemoteEvents = listOf(EventIdAndRevision(remoteEventForLocalEvent1))
        ))
        every { compensatingActionExecutor.execute(compensatingAction2) }.returns(ExecutionResult(
                success = true,
                newLocalEvents = listOf(EventIdAndRevision(localEventForRemoteEvent2)),
                newRemoteEvents = listOf(EventIdAndRevision(remoteEventForLocalEvent2))
        ))
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            compensatingActionExecutor.execute(compensatingAction1)
        }
        verify(exactly = 0) {
            remoteEvents.removeEvent(remoteEvent1)
            localEvents.removeEvent(localEvent1)
            compensatingActionExecutor.execute(compensatingAction2)
            remoteEvents.removeEvent(remoteEvent2)
            localEvents.removeEvent(localEvent2)
        }
        assertThat(stateObserver.values().toList()).isEmpty()
//        val finalState = stateObserver.values().last()
//        assertThat(finalState.lastKnownLocalRevisions[localEvent1.noteId]).isEqualTo(localEvent2.revision)
//        assertThat(finalState.lastKnownRemoteRevisions[localEvent1.noteId]).isEqualTo(remoteEvent2.revision)
//        assertThat(finalState.lastSynchronizedLocalRevisions[localEvent1.noteId]).isEqualTo(localEvent2.revision)
    }

    @Test
    fun `one note, multiple compensating actions, different events in compensating actions`() {
        // Given
        val localEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val localEvent2 = modelEvent(eventId = 12, noteId = 1, revision = 2)
        val remoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val remoteEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 12)
        val localEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val localEventForRemoteEvent2 = modelEvent(eventId = 14, noteId = 1, revision = 4)
        val remoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val remoteEventForLocalEvent2 = modelEvent(eventId = 4, noteId = 1, revision = 14)
        every { localEvents.getEvents() }.returns(Observable.just(localEvent1, localEvent2))
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteEvent1, remoteEvent2))
        val compensatingAction1 = CompensatingAction(
                compensatedLocalEvents = listOf(localEvent1),
                compensatedRemoteEvents = listOf(remoteEvent1),
                newLocalEvents = listOf(localEventForRemoteEvent1),
                newRemoteEvents = listOf(remoteEventForLocalEvent1)
        )
        val compensatingAction2 = CompensatingAction(
                compensatedLocalEvents = listOf(localEvent2),
                compensatedRemoteEvents = listOf(), // Missing remoteEvent2
                newLocalEvents = listOf(localEventForRemoteEvent2),
                newRemoteEvents = listOf(remoteEventForLocalEvent2)
        )
        every {
            synchronizationStrategy.resolve(
                    localEvents = listOf(localEvent1, localEvent2),
                    remoteEvents = listOf(remoteEvent1, remoteEvent2)
            )
        }.returns(Solution(listOf(compensatingAction1, compensatingAction2)))
        every { compensatingActionExecutor.execute(compensatingAction1) }.returns(ExecutionResult(
                success = true,
                newLocalEvents = listOf(EventIdAndRevision(localEventForRemoteEvent1)),
                newRemoteEvents = listOf(EventIdAndRevision(remoteEventForLocalEvent1))
        ))
        every { compensatingActionExecutor.execute(compensatingAction2) }.returns(ExecutionResult(
                success = true,
                newLocalEvents = listOf(EventIdAndRevision(localEventForRemoteEvent2)),
                newRemoteEvents = listOf(EventIdAndRevision(remoteEventForLocalEvent2))
        ))
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
        }
        verify(exactly = 0) {
            compensatingActionExecutor.execute(compensatingAction1)
            remoteEvents.removeEvent(remoteEvent1)
            localEvents.removeEvent(localEvent1)
            compensatingActionExecutor.execute(compensatingAction2)
            remoteEvents.removeEvent(remoteEvent2)
            localEvents.removeEvent(localEvent2)
        }
        assertThat(stateObserver.values().toList()).isEmpty()
//        val finalState = stateObserver.values().last()
//        assertThat(finalState.lastKnownLocalRevisions[localEvent1.noteId]).isEqualTo(localEvent2.revision)
//        assertThat(finalState.lastKnownRemoteRevisions[localEvent1.noteId]).isEqualTo(remoteEvent2.revision)
//        assertThat(finalState.lastSynchronizedLocalRevisions[localEvent1.noteId]).isEqualTo(localEvent2.revision)
    }

    @Test
    fun `multiple notes, multiple compensating actions`() {
        // Given
        val localEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val localEvent2 = modelEvent(eventId = 12, noteId = 2, revision = 2)
        val remoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val remoteEvent2 = modelEvent(eventId = 2, noteId = 2, revision = 12)
        val localEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val localEventForRemoteEvent2 = modelEvent(eventId = 14, noteId = 2, revision = 4)
        val remoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val remoteEventForLocalEvent2 = modelEvent(eventId = 4, noteId = 2, revision = 14)
        every { localEvents.getEvents() }.returns(Observable.just(localEvent1, localEvent2))
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteEvent1, remoteEvent2))
        val compensatingAction1 = CompensatingAction(
                compensatedLocalEvents = listOf(localEvent1),
                compensatedRemoteEvents = listOf(remoteEvent1),
                newLocalEvents = listOf(localEventForRemoteEvent1),
                newRemoteEvents = listOf(remoteEventForLocalEvent1)
        )
        val compensatingAction2 = CompensatingAction(
                compensatedLocalEvents = listOf(localEvent2),
                compensatedRemoteEvents = listOf(remoteEvent2),
                newLocalEvents = listOf(localEventForRemoteEvent2),
                newRemoteEvents = listOf(remoteEventForLocalEvent2)
        )
        every {
            synchronizationStrategy.resolve(
                    localEvents = listOf(localEvent1),
                    remoteEvents = listOf(remoteEvent1)
            )
        }.returns(Solution(listOf(compensatingAction1)))
        every {
            synchronizationStrategy.resolve(
                    localEvents = listOf(localEvent2),
                    remoteEvents = listOf(remoteEvent2)
            )
        }.returns(Solution(listOf(compensatingAction2)))
        every { compensatingActionExecutor.execute(compensatingAction1) }.returns(ExecutionResult(
                success = true,
                newLocalEvents = listOf(EventIdAndRevision(localEventForRemoteEvent1)),
                newRemoteEvents = listOf(EventIdAndRevision(remoteEventForLocalEvent1))
        ))
        every { compensatingActionExecutor.execute(compensatingAction2) }.returns(ExecutionResult(
                success = true,
                newLocalEvents = listOf(EventIdAndRevision(localEventForRemoteEvent2)),
                newRemoteEvents = listOf(EventIdAndRevision(remoteEventForLocalEvent2))
        ))
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            compensatingActionExecutor.execute(compensatingAction1)
            remoteEvents.removeEvent(remoteEvent1)
            localEvents.removeEvent(localEvent1)
            compensatingActionExecutor.execute(compensatingAction2)
            remoteEvents.removeEvent(remoteEvent2)
            localEvents.removeEvent(localEvent2)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[localEvent1.noteId]).isEqualTo(localEventForRemoteEvent1.revision)
        assertThat(finalState.lastKnownLocalRevisions[localEvent2.noteId]).isEqualTo(localEventForRemoteEvent2.revision)
        assertThat(finalState.lastKnownRemoteRevisions[localEvent1.noteId]).isEqualTo(remoteEventForLocalEvent1.revision)
        assertThat(finalState.lastKnownRemoteRevisions[localEvent2.noteId]).isEqualTo(remoteEventForLocalEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[localEvent1.noteId]).isEqualTo(localEventForRemoteEvent1.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[localEvent2.noteId]).isEqualTo(localEventForRemoteEvent2.revision)
    }

    @Test
    fun `multiple notes, multiple compensating actions, executing one action fails`() {
        // Given
        val localEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val localEvent2 = modelEvent(eventId = 12, noteId = 2, revision = 2)
        val remoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val remoteEvent2 = modelEvent(eventId = 2, noteId = 2, revision = 12)
        val localEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val localEventForRemoteEvent2 = modelEvent(eventId = 14, noteId = 2, revision = 4)
        val remoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val remoteEventForLocalEvent2 = modelEvent(eventId = 4, noteId = 2, revision = 14)
        every { localEvents.getEvents() }.returns(Observable.just(localEvent1, localEvent2))
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteEvent1, remoteEvent2))
        val compensatingAction1 = CompensatingAction(
                compensatedLocalEvents = listOf(localEvent1),
                compensatedRemoteEvents = listOf(remoteEvent1),
                newLocalEvents = listOf(localEventForRemoteEvent1),
                newRemoteEvents = listOf(remoteEventForLocalEvent1)
        )
        val compensatingAction2 = CompensatingAction(
                compensatedLocalEvents = listOf(localEvent2),
                compensatedRemoteEvents = listOf(remoteEvent2),
                newLocalEvents = listOf(localEventForRemoteEvent2),
                newRemoteEvents = listOf(remoteEventForLocalEvent2)
        )
        every {
            synchronizationStrategy.resolve(
                    localEvents = listOf(localEvent1),
                    remoteEvents = listOf(remoteEvent1)
            )
        }.returns(Solution(listOf(compensatingAction1)))
        every {
            synchronizationStrategy.resolve(
                    localEvents = listOf(localEvent2),
                    remoteEvents = listOf(remoteEvent2)
            )
        }.returns(Solution(listOf(compensatingAction2)))
        every { compensatingActionExecutor.execute(compensatingAction1) }.returns(ExecutionResult(
                success = false,
                newLocalEvents = emptyList(), // Local failed
                newRemoteEvents = listOf(EventIdAndRevision(remoteEventForLocalEvent1))
        ))
        every { compensatingActionExecutor.execute(compensatingAction2) }.returns(ExecutionResult(
                success = true,
                newLocalEvents = listOf(EventIdAndRevision(localEventForRemoteEvent1)),
                newRemoteEvents = listOf(EventIdAndRevision(remoteEventForLocalEvent2))
        ))
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            compensatingActionExecutor.execute(compensatingAction1)
            compensatingActionExecutor.execute(compensatingAction2)
            remoteEvents.removeEvent(remoteEvent2)
            localEvents.removeEvent(localEvent2)
        }
        verify(exactly = 0) {
            remoteEvents.removeEvent(remoteEvent1)
            localEvents.removeEvent(localEvent1)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[localEvent1.noteId]).isNull()
        assertThat(finalState.lastKnownRemoteRevisions[localEvent1.noteId]).isNull()
        assertThat(finalState.lastSynchronizedLocalRevisions[localEvent1.noteId]).isNull()
//        assertThat(finalState.lastKnownLocalRevisions[localEvent1.noteId]).isEqualTo(localEvent2.revision)
//        assertThat(finalState.lastKnownRemoteRevisions[localEvent1.noteId]).isEqualTo(remoteEventForLocalEvent1.revision)
//        assertThat(finalState.lastSynchronizedLocalRevisions[localEvent1.noteId]).isEqualTo(1)
    }

    @Disabled
    @Test
    fun `do not synchronize events created during synchronization`() {
        // Given
        val localEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val remoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val localEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val remoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        every { localEvents.getEvents() }.returns(Observable.just(localEvent1))
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteEvent1))
        val compensatingAction = CompensatingAction(
                compensatedLocalEvents = listOf(localEvent1),
                compensatedRemoteEvents = listOf(remoteEvent1),
                newLocalEvents = listOf(localEventForRemoteEvent1),
                newRemoteEvents = listOf(remoteEventForLocalEvent1)
        )
        every {
            synchronizationStrategy.resolve(
                    localEvents = listOf(localEvent1),
                    remoteEvents = listOf(remoteEvent1)
            )
        }.returns(Solution(compensatingAction))
        every { compensatingActionExecutor.execute(compensatingAction) }.returns(ExecutionResult(
                success = true,
                newLocalEvents = listOf(EventIdAndRevision(localEventForRemoteEvent1)),
                newRemoteEvents = listOf(EventIdAndRevision(remoteEventForLocalEvent1))
        ))
        val s = createSynchronizer()
        s.synchronize()
        every { localEvents.getEvents() }.returns(Observable.just(localEventForRemoteEvent1))
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteEventForLocalEvent1))

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteEvents.removeEvent(remoteEvent1)
            localEvents.removeEvent(localEvent1)
            compensatingActionExecutor.execute(compensatingAction)
            remoteEvents.removeEvent(remoteEventForLocalEvent1)
            localEvents.removeEvent(localEventForRemoteEvent1)
        }
    }

    private fun createSynchronizer(
            localEvents: ModifiableEventRepository = this.localEvents,
            remoteEvents: ModifiableEventRepository = this.remoteEvents
    ) =
            NewSynchronizer(
                    localEvents,
                    remoteEvents,
                    synchronizationStrategy,
                    compensatingActionExecutor,
                    initialState
            )

    companion object {
        internal fun modelEvent(eventId: Int, noteId: Int, revision: Int): NoteCreatedEvent {
            return NoteCreatedEvent(eventId = eventId, noteId = "note-$noteId", revision = revision, title = "Title $noteId")
        }
    }
}
