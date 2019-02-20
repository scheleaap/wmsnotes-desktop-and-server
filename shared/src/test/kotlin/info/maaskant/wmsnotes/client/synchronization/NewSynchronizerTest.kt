package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.synchronization.SynchronizationStrategy.ResolutionResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.SynchronizationStrategy.ResolutionResult.Solution
import info.maaskant.wmsnotes.client.synchronization.commandexecutor.CommandExecutor
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.model.*
import io.mockk.*
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.rxkotlin.toObservable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class NewSynchronizerTest {

    private val localEvents: ModifiableEventRepository = mockk()
    private val remoteEvents: ModifiableEventRepository = mockk()
    private val synchronizationStrategy: SynchronizationStrategy = mockk()
    private val eventToCommandMapper: EventToCommandMapper = mockk()
    private val localCommandExecutor: CommandExecutor = mockk()
    private val remoteCommandExecutor: CommandExecutor = mockk()
    private lateinit var initialState: SynchronizerState

    @BeforeEach
    fun init() {
        clearMocks(
                localEvents,
                remoteEvents,
                synchronizationStrategy
        )
        every { localCommandExecutor.execute(any()) }.returns(CommandExecutor.ExecutionResult.Failure)
        every { remoteCommandExecutor.execute(any()) }.returns(CommandExecutor.ExecutionResult.Failure)
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
            synchronizationStrategy.resolve(any(), any(), any()).wasNot(Called)
            localCommandExecutor.execute(any()).wasNot(Called)
            remoteCommandExecutor.execute(any()).wasNot(Called)
        }
    }

    @Test
    fun `nothing happens if synchronization strategy has no solution`() {
        // Given
        val localOutboundEvent: Event = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val noteId = localOutboundEvent.noteId
        every { localEvents.getEvents() }.returns(Observable.just(localOutboundEvent))
        every { remoteEvents.getEvents() }.returns(Observable.empty())
        every { synchronizationStrategy.resolve(any(), any(), any()) }.returns(NoSolution)
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verify {
            synchronizationStrategy.resolve(noteId, listOf(localOutboundEvent), emptyList())
        }
        verify(exactly = 0) {
            localCommandExecutor.execute(any())
            remoteCommandExecutor.execute(any())
            localEvents.removeEvent(any())
            remoteEvents.removeEvent(any())
        }
    }

    @Test
    fun `one note, one compensating action, only local events`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, noteId = 1, revision = 2)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, noteId = 1, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, noteId = 1, revision = 14)
        val noteId = compensatedLocalEvent1.noteId
        initialState = initialState
                .updateLastKnownRemoteRevision(noteId, newRemoteEventForLocalEvent1.revision - 1)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents()
        every {
            synchronizationStrategy.resolve(
                    noteId = noteId,
                    localEvents = listOf(compensatedLocalEvent1, compensatedLocalEvent2),
                    remoteEvents = emptyList()
            )
        }.returns(Solution(CompensatingAction(
                compensatedLocalEvents = listOf(compensatedLocalEvent1, compensatedLocalEvent2),
                compensatedRemoteEvents = emptyList(),
                newLocalEvents = emptyList(),
                newRemoteEvents = listOf(compensatingEventForLocalEvent1, compensatingEventForLocalEvent2)
        )
        ))
        val commandForLocalEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent1,
                lastRevision = initialState.lastKnownRemoteRevisions[noteId],
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent1
        )
        val commandForLocalEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent2,
                lastRevision = newRemoteEventForLocalEvent1.revision,
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent2
        )
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1)
            remoteCommandExecutor.execute(commandForLocalEvent2)
            localEvents.removeEvent(compensatedLocalEvent1)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownRemoteRevisions[noteId]).isEqualTo(newRemoteEventForLocalEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[noteId]).isEqualTo(compensatedLocalEvent2.revision)
    }

    @Test
    fun `one note, one compensating action, only remote events`() {
        // Given
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 12)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, noteId = 1, revision = 0)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, noteId = 1, revision = 4)
        val noteId = compensatedRemoteEvent1.noteId
        initialState = initialState
                .updateLastKnownLocalRevision(noteId, newLocalEventForRemoteEvent1.revision - 1)
        every { localEvents.getEvents() }.returns(Observable.empty())
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    noteId = noteId,
                    localEvents = emptyList(),
                    remoteEvents = listOf(compensatedRemoteEvent1, compensatedRemoteEvent2)
            )
        }.returns(Solution(CompensatingAction(
                compensatedLocalEvents = emptyList(),
                compensatedRemoteEvents = listOf(compensatedRemoteEvent1, compensatedRemoteEvent2),
                newLocalEvents = listOf(compensatingEventForRemoteEvent1, compensatingEventForRemoteEvent2),
                newRemoteEvents = emptyList()
        )
        ))
        val commandForRemoteEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent1,
                lastRevision = initialState.lastKnownLocalRevisions[noteId],
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent1
        )
        val commandForRemoteEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent2,
                lastRevision = newLocalEventForRemoteEvent1.revision,
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent2
        )
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            localCommandExecutor.execute(commandForRemoteEvent1)
            localCommandExecutor.execute(commandForRemoteEvent2)
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[noteId]).isEqualTo(newLocalEventForRemoteEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[noteId]).isEqualTo(newLocalEventForRemoteEvent2.revision)
    }

    @Test
    fun `one note, one compensating action`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, noteId = 1, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, noteId = 1, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, noteId = 1, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, noteId = 1, revision = 14)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, noteId = 1, revision = 4)
        val noteId = compensatedLocalEvent1.noteId
        initialState = initialState
                .updateLastKnownLocalRevision(noteId, newLocalEventForRemoteEvent1.revision - 1)
                .updateLastKnownRemoteRevision(noteId, newRemoteEventForLocalEvent1.revision - 1)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    noteId = noteId,
                    localEvents = listOf(compensatedLocalEvent1, compensatedLocalEvent2),
                    remoteEvents = listOf(compensatedRemoteEvent1, compensatedRemoteEvent2)
            )
        }.returns(Solution(CompensatingAction(
                compensatedLocalEvents = listOf(compensatedLocalEvent1, compensatedLocalEvent2),
                compensatedRemoteEvents = listOf(compensatedRemoteEvent1, compensatedRemoteEvent2),
                newLocalEvents = listOf(compensatingEventForRemoteEvent1, compensatingEventForRemoteEvent2),
                newRemoteEvents = listOf(compensatingEventForLocalEvent1, compensatingEventForLocalEvent2)
        )))
        val commandForLocalEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent1,
                lastRevision = initialState.lastKnownRemoteRevisions[noteId],
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent1
        )
        val commandForLocalEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent2,
                lastRevision = newRemoteEventForLocalEvent1.revision,
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent2
        )
        val commandForRemoteEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent1,
                lastRevision = initialState.lastKnownLocalRevisions[noteId],
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent1
        )
        val commandForRemoteEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent2,
                lastRevision = newLocalEventForRemoteEvent1.revision,
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent2
        )
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1)
            remoteCommandExecutor.execute(commandForLocalEvent2)
            localCommandExecutor.execute(commandForRemoteEvent1)
            localCommandExecutor.execute(commandForRemoteEvent2)
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
            localEvents.removeEvent(compensatedLocalEvent1)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[noteId]).isEqualTo(newLocalEventForRemoteEvent2.revision)
        assertThat(finalState.lastKnownRemoteRevisions[noteId]).isEqualTo(newRemoteEventForLocalEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[noteId]).isEqualTo(newLocalEventForRemoteEvent2.revision)
    }

    @Test
    fun `one note, multiple compensating actions`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, noteId = 1, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, noteId = 1, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, noteId = 1, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, noteId = 1, revision = 14)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, noteId = 1, revision = 4)
        val noteId = compensatedLocalEvent1.noteId
        initialState = initialState
                .updateLastKnownLocalRevision(noteId, newLocalEventForRemoteEvent1.revision - 1)
                .updateLastKnownRemoteRevision(noteId, newRemoteEventForLocalEvent1.revision - 1)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    noteId = noteId,
                    localEvents = listOf(compensatedLocalEvent1, compensatedLocalEvent2),
                    remoteEvents = listOf(compensatedRemoteEvent1, compensatedRemoteEvent2)
            )
        }.returns(Solution(listOf(
                CompensatingAction(
                        compensatedLocalEvents = listOf(compensatedLocalEvent1),
                        compensatedRemoteEvents = listOf(compensatedRemoteEvent1),
                        newLocalEvents = listOf(compensatingEventForRemoteEvent1),
                        newRemoteEvents = listOf(compensatingEventForLocalEvent1)
                ),
                CompensatingAction(
                        compensatedLocalEvents = listOf(compensatedLocalEvent2),
                        compensatedRemoteEvents = listOf(compensatedRemoteEvent2),
                        newLocalEvents = listOf(compensatingEventForRemoteEvent2),
                        newRemoteEvents = listOf(compensatingEventForLocalEvent2)
                )
        )))
        val commandForLocalEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent1,
                lastRevision = initialState.lastKnownRemoteRevisions[noteId],
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent1
        )
        val commandForLocalEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent2,
                lastRevision = newRemoteEventForLocalEvent1.revision,
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent2
        )
        val commandForRemoteEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent1,
                lastRevision = initialState.lastKnownLocalRevisions[noteId],
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent1
        )
        val commandForRemoteEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent2,
                lastRevision = newLocalEventForRemoteEvent1.revision,
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent2
        )
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1)
            localCommandExecutor.execute(commandForRemoteEvent1)
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            localEvents.removeEvent(compensatedLocalEvent1)
            remoteCommandExecutor.execute(commandForLocalEvent2)
            localCommandExecutor.execute(commandForRemoteEvent2)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[noteId]).isEqualTo(newLocalEventForRemoteEvent2.revision)
        assertThat(finalState.lastKnownRemoteRevisions[noteId]).isEqualTo(newRemoteEventForLocalEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[noteId]).isEqualTo(newLocalEventForRemoteEvent2.revision)
    }

    @Test
    fun `one note, multiple compensating actions, executing local command fails`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, noteId = 1, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, noteId = 1, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, noteId = 1, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, noteId = 1, revision = 14)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, noteId = 1, revision = 4)
        val noteId = compensatedLocalEvent1.noteId
        initialState = initialState
                .updateLastKnownLocalRevision(noteId, newLocalEventForRemoteEvent1.revision - 1)
                .updateLastKnownRemoteRevision(noteId, newRemoteEventForLocalEvent1.revision - 1)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    noteId = noteId,
                    localEvents = listOf(compensatedLocalEvent1, compensatedLocalEvent2),
                    remoteEvents = listOf(compensatedRemoteEvent1, compensatedRemoteEvent2)
            )
        }.returns(Solution(listOf(
                CompensatingAction(
                        compensatedLocalEvents = listOf(compensatedLocalEvent1),
                        compensatedRemoteEvents = listOf(compensatedRemoteEvent1),
                        newLocalEvents = listOf(compensatingEventForRemoteEvent1),
                        newRemoteEvents = listOf(compensatingEventForLocalEvent1)
                ),
                CompensatingAction(
                        compensatedLocalEvents = listOf(compensatedLocalEvent2),
                        compensatedRemoteEvents = listOf(compensatedRemoteEvent2),
                        newLocalEvents = listOf(compensatingEventForRemoteEvent2),
                        newRemoteEvents = listOf(compensatingEventForLocalEvent2)
                )
        )))
        val commandForLocalEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent1,
                lastRevision = initialState.lastKnownRemoteRevisions[noteId],
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent1
        )
        val commandForLocalEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent2,
                lastRevision = newRemoteEventForLocalEvent1.revision,
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent2
        )
        val commandForRemoteEvent1 = givenTheFailedExecutionOfACompensatingEvent( // Failure
                compensatingEvent = compensatingEventForRemoteEvent1,
                lastRevision = initialState.lastKnownLocalRevisions[noteId],
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent1
        )
        val commandForRemoteEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent2,
                lastRevision = newLocalEventForRemoteEvent1.revision,
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent2
        )
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1)
            localCommandExecutor.execute(commandForRemoteEvent1)
        }
        verify(exactly = 0) {
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            localEvents.removeEvent(compensatedLocalEvent1)
            remoteCommandExecutor.execute(commandForLocalEvent2)
            localCommandExecutor.execute(commandForRemoteEvent2)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[noteId]).isEqualTo(initialState.lastKnownLocalRevisions[noteId])
        assertThat(finalState.lastKnownRemoteRevisions[noteId]).isEqualTo(newRemoteEventForLocalEvent1.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[noteId]).isEqualTo(initialState.lastSynchronizedLocalRevisions[noteId])
    }

    @Test
    fun `one note, multiple compensating actions, executing remote command fails`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, noteId = 1, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, noteId = 1, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, noteId = 1, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, noteId = 1, revision = 14)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, noteId = 1, revision = 4)
        val noteId = compensatedLocalEvent1.noteId
        initialState = initialState
                .updateLastKnownLocalRevision(noteId, newLocalEventForRemoteEvent1.revision - 1)
                .updateLastKnownRemoteRevision(noteId, newRemoteEventForLocalEvent1.revision - 1)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    noteId = noteId,
                    localEvents = listOf(compensatedLocalEvent1, compensatedLocalEvent2),
                    remoteEvents = listOf(compensatedRemoteEvent1, compensatedRemoteEvent2)
            )
        }.returns(Solution(listOf(
                CompensatingAction(
                        compensatedLocalEvents = listOf(compensatedLocalEvent1),
                        compensatedRemoteEvents = listOf(compensatedRemoteEvent1),
                        newLocalEvents = listOf(compensatingEventForRemoteEvent1),
                        newRemoteEvents = listOf(compensatingEventForLocalEvent1)
                ),
                CompensatingAction(
                        compensatedLocalEvents = listOf(compensatedLocalEvent2),
                        compensatedRemoteEvents = listOf(compensatedRemoteEvent2),
                        newLocalEvents = listOf(compensatingEventForRemoteEvent2),
                        newRemoteEvents = listOf(compensatingEventForLocalEvent2)
                )
        )))
        val commandForLocalEvent1 = givenTheFailedExecutionOfACompensatingEvent( // Failure
                compensatingEvent = compensatingEventForLocalEvent1,
                lastRevision = initialState.lastKnownRemoteRevisions[noteId],
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent1
        )
        val commandForLocalEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent2,
                lastRevision = newRemoteEventForLocalEvent1.revision,
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent2
        )
        val commandForRemoteEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent1,
                lastRevision = initialState.lastKnownLocalRevisions[noteId],
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent1
        )
        val commandForRemoteEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent2,
                lastRevision = newLocalEventForRemoteEvent1.revision,
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent2
        )
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1)
        }
        verify(exactly = 0) {
            localCommandExecutor.execute(commandForRemoteEvent1)
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            localEvents.removeEvent(compensatedLocalEvent1)
            remoteCommandExecutor.execute(commandForLocalEvent2)
            localCommandExecutor.execute(commandForRemoteEvent2)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        assertThatTheStateDidNotChange(stateObserver)
    }

    @Test
    fun `one note, multiple compensating actions, different events in compensating actions`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, noteId = 1, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, noteId = 1, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, noteId = 1, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, noteId = 1, revision = 14)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, noteId = 1, revision = 4)
        val noteId = compensatedLocalEvent1.noteId
        initialState = initialState
                .updateLastKnownLocalRevision(noteId, newLocalEventForRemoteEvent1.revision - 1)
                .updateLastKnownRemoteRevision(noteId, newRemoteEventForLocalEvent1.revision - 1)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    noteId = noteId,
                    localEvents = listOf(compensatedLocalEvent1, compensatedLocalEvent2),
                    remoteEvents = listOf(compensatedRemoteEvent1, compensatedRemoteEvent2)
            )
        }.returns(Solution(listOf(
                CompensatingAction(
                        compensatedLocalEvents = listOf(compensatedLocalEvent1),
                        compensatedRemoteEvents = listOf(compensatedRemoteEvent1),
                        newLocalEvents = listOf(compensatingEventForRemoteEvent1),
                        newRemoteEvents = listOf(compensatingEventForLocalEvent1)
                ),
                CompensatingAction(
                        compensatedLocalEvents = emptyList(), // Missing compensatedLocalEvent2
                        compensatedRemoteEvents = listOf(compensatedRemoteEvent2),
                        newLocalEvents = listOf(compensatingEventForRemoteEvent2),
                        newRemoteEvents = listOf(compensatingEventForLocalEvent2)
                )
        )))
        givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent1,
                lastRevision = initialState.lastKnownRemoteRevisions[noteId],
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent1
        )
        givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent2,
                lastRevision = newRemoteEventForLocalEvent1.revision,
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent2
        )
        givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent1,
                lastRevision = initialState.lastKnownLocalRevisions[noteId],
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent1
        )
        givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent2,
                lastRevision = newLocalEventForRemoteEvent1.revision,
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent2
        )
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verify(exactly = 0) {
            localCommandExecutor.execute(any())
            remoteCommandExecutor.execute(any())
            localEvents.removeEvent(any())
            remoteEvents.removeEvent(any())
        }
        assertThatTheStateDidNotChange(stateObserver)
    }

    @Test
    fun `multiple notes, multiple compensating actions`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, noteId = 2, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, noteId = 2, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, noteId = 2, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, noteId = 2, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, noteId = 2, revision = 14)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, noteId = 2, revision = 4)
        val noteId1 = compensatedLocalEvent1.noteId
        val noteId2 = compensatedLocalEvent2.noteId
        initialState = initialState
                .updateLastKnownLocalRevision(noteId1, newLocalEventForRemoteEvent1.revision - 1)
                .updateLastKnownLocalRevision(noteId2, newLocalEventForRemoteEvent2.revision - 1)
                .updateLastKnownRemoteRevision(noteId1, newRemoteEventForLocalEvent1.revision - 1)
                .updateLastKnownRemoteRevision(noteId2, newRemoteEventForLocalEvent2.revision - 1)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    noteId = noteId1,
                    localEvents = listOf(compensatedLocalEvent1),
                    remoteEvents = listOf(compensatedRemoteEvent1)
            )
        }.returns(Solution(listOf(
                CompensatingAction(
                        compensatedLocalEvents = listOf(compensatedLocalEvent1),
                        compensatedRemoteEvents = listOf(compensatedRemoteEvent1),
                        newLocalEvents = listOf(compensatingEventForRemoteEvent1),
                        newRemoteEvents = listOf(compensatingEventForLocalEvent1)
                )
        )))
        every {
            synchronizationStrategy.resolve(
                    noteId = noteId2,
                    localEvents = listOf(compensatedLocalEvent2),
                    remoteEvents = listOf(compensatedRemoteEvent2)
            )
        }.returns(Solution(listOf(
                CompensatingAction(
                        compensatedLocalEvents = listOf(compensatedLocalEvent2),
                        compensatedRemoteEvents = listOf(compensatedRemoteEvent2),
                        newLocalEvents = listOf(compensatingEventForRemoteEvent2),
                        newRemoteEvents = listOf(compensatingEventForLocalEvent2)
                )
        )))
        val commandForLocalEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent1,
                lastRevision = initialState.lastKnownRemoteRevisions[noteId1],
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent1
        )
        val commandForLocalEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent2,
                lastRevision = initialState.lastKnownRemoteRevisions[noteId2],
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent2
        )
        val commandForRemoteEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent1,
                lastRevision = initialState.lastKnownLocalRevisions[noteId1],
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent1
        )
        val commandForRemoteEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent2,
                lastRevision = initialState.lastKnownLocalRevisions[noteId2],
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent2
        )
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1)
            localCommandExecutor.execute(commandForRemoteEvent1)
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            localEvents.removeEvent(compensatedLocalEvent1)
            remoteCommandExecutor.execute(commandForLocalEvent2)
            localCommandExecutor.execute(commandForRemoteEvent2)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[noteId1]).isEqualTo(newLocalEventForRemoteEvent1.revision)
        assertThat(finalState.lastKnownLocalRevisions[noteId2]).isEqualTo(newLocalEventForRemoteEvent2.revision)
        assertThat(finalState.lastKnownRemoteRevisions[noteId1]).isEqualTo(newRemoteEventForLocalEvent1.revision)
        assertThat(finalState.lastKnownRemoteRevisions[noteId2]).isEqualTo(newRemoteEventForLocalEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[noteId1]).isEqualTo(newLocalEventForRemoteEvent1.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[noteId2]).isEqualTo(newLocalEventForRemoteEvent2.revision)
    }

    @Test
    fun `multiple notes, multiple compensating actions, executing local command fails`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, noteId = 2, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, noteId = 2, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, noteId = 2, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, noteId = 2, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, noteId = 2, revision = 14)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, noteId = 2, revision = 4)
        val noteId1 = compensatedLocalEvent1.noteId
        val noteId2 = compensatedLocalEvent2.noteId
        initialState = initialState
                .updateLastKnownLocalRevision(noteId1, newLocalEventForRemoteEvent1.revision - 1)
                .updateLastKnownLocalRevision(noteId2, newLocalEventForRemoteEvent2.revision - 1)
                .updateLastKnownRemoteRevision(noteId1, newRemoteEventForLocalEvent1.revision - 1)
                .updateLastKnownRemoteRevision(noteId2, newRemoteEventForLocalEvent2.revision - 1)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    noteId = noteId1,
                    localEvents = listOf(compensatedLocalEvent1),
                    remoteEvents = listOf(compensatedRemoteEvent1)
            )
        }.returns(Solution(listOf(
                CompensatingAction(
                        compensatedLocalEvents = listOf(compensatedLocalEvent1),
                        compensatedRemoteEvents = listOf(compensatedRemoteEvent1),
                        newLocalEvents = listOf(compensatingEventForRemoteEvent1),
                        newRemoteEvents = listOf(compensatingEventForLocalEvent1)
                )
        )))
        every {
            synchronizationStrategy.resolve(
                    noteId = noteId2,
                    localEvents = listOf(compensatedLocalEvent2),
                    remoteEvents = listOf(compensatedRemoteEvent2)
            )
        }.returns(Solution(listOf(
                CompensatingAction(
                        compensatedLocalEvents = listOf(compensatedLocalEvent2),
                        compensatedRemoteEvents = listOf(compensatedRemoteEvent2),
                        newLocalEvents = listOf(compensatingEventForRemoteEvent2),
                        newRemoteEvents = listOf(compensatingEventForLocalEvent2)
                )
        )))
        val commandForLocalEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent1,
                lastRevision = initialState.lastKnownRemoteRevisions[noteId1],
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent1
        )
        val commandForLocalEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent2,
                lastRevision = initialState.lastKnownRemoteRevisions[noteId2],
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent2
        )
        val commandForRemoteEvent1 = givenTheFailedExecutionOfACompensatingEvent( // Failure
                compensatingEvent = compensatingEventForRemoteEvent1,
                lastRevision = initialState.lastKnownLocalRevisions[noteId1],
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent1
        )
        val commandForRemoteEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent2,
                lastRevision = initialState.lastKnownLocalRevisions[noteId2],
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent2
        )
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1)
            localCommandExecutor.execute(commandForRemoteEvent1)
            remoteCommandExecutor.execute(commandForLocalEvent2)
            localCommandExecutor.execute(commandForRemoteEvent2)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        verify(exactly = 0) {
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            localEvents.removeEvent(compensatedLocalEvent1)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[noteId1]).isEqualTo(initialState.lastKnownLocalRevisions[noteId1])
        assertThat(finalState.lastKnownLocalRevisions[noteId2]).isEqualTo(newLocalEventForRemoteEvent2.revision)
        assertThat(finalState.lastKnownRemoteRevisions[noteId1]).isEqualTo(newRemoteEventForLocalEvent1.revision)
        assertThat(finalState.lastKnownRemoteRevisions[noteId2]).isEqualTo(newRemoteEventForLocalEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[noteId1]).isEqualTo(initialState.lastSynchronizedLocalRevisions[noteId1])
        assertThat(finalState.lastSynchronizedLocalRevisions[noteId2]).isEqualTo(newLocalEventForRemoteEvent2.revision)
    }

    @Test
    fun `multiple notes, multiple compensating actions, executing remote command fails`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, noteId = 2, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, noteId = 2, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, noteId = 2, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, noteId = 2, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, noteId = 2, revision = 14)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, noteId = 2, revision = 4)
        val noteId1 = compensatedLocalEvent1.noteId
        val noteId2 = compensatedLocalEvent2.noteId
        initialState = initialState
                .updateLastKnownLocalRevision(noteId1, newLocalEventForRemoteEvent1.revision - 1)
                .updateLastKnownLocalRevision(noteId2, newLocalEventForRemoteEvent2.revision - 1)
                .updateLastKnownRemoteRevision(noteId1, newRemoteEventForLocalEvent1.revision - 1)
                .updateLastKnownRemoteRevision(noteId2, newRemoteEventForLocalEvent2.revision - 1)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    noteId = noteId1,
                    localEvents = listOf(compensatedLocalEvent1),
                    remoteEvents = listOf(compensatedRemoteEvent1)
            )
        }.returns(Solution(listOf(
                CompensatingAction(
                        compensatedLocalEvents = listOf(compensatedLocalEvent1),
                        compensatedRemoteEvents = listOf(compensatedRemoteEvent1),
                        newLocalEvents = listOf(compensatingEventForRemoteEvent1),
                        newRemoteEvents = listOf(compensatingEventForLocalEvent1)
                )
        )))
        every {
            synchronizationStrategy.resolve(
                    noteId = noteId2,
                    localEvents = listOf(compensatedLocalEvent2),
                    remoteEvents = listOf(compensatedRemoteEvent2)
            )
        }.returns(Solution(listOf(
                CompensatingAction(
                        compensatedLocalEvents = listOf(compensatedLocalEvent2),
                        compensatedRemoteEvents = listOf(compensatedRemoteEvent2),
                        newLocalEvents = listOf(compensatingEventForRemoteEvent2),
                        newRemoteEvents = listOf(compensatingEventForLocalEvent2)
                )
        )))
        val commandForLocalEvent1 = givenTheFailedExecutionOfACompensatingEvent( // Failure
                compensatingEvent = compensatingEventForLocalEvent1,
                lastRevision = initialState.lastKnownRemoteRevisions[noteId1],
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent1
        )
        val commandForLocalEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent2,
                lastRevision = initialState.lastKnownRemoteRevisions[noteId2],
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent2
        )
        val commandForRemoteEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent1,
                lastRevision = initialState.lastKnownLocalRevisions[noteId1],
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent1
        )
        val commandForRemoteEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent2,
                lastRevision = initialState.lastKnownLocalRevisions[noteId2],
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent2
        )
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1)
            remoteCommandExecutor.execute(commandForLocalEvent2)
            localCommandExecutor.execute(commandForRemoteEvent2)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        verify(exactly = 0) {
            localCommandExecutor.execute(commandForRemoteEvent1)
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            localEvents.removeEvent(compensatedLocalEvent1)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[noteId1]).isEqualTo(initialState.lastKnownLocalRevisions[noteId1])
        assertThat(finalState.lastKnownLocalRevisions[noteId2]).isEqualTo(newLocalEventForRemoteEvent2.revision)
        assertThat(finalState.lastKnownRemoteRevisions[noteId1]).isEqualTo(initialState.lastKnownRemoteRevisions[noteId1])
        assertThat(finalState.lastKnownRemoteRevisions[noteId2]).isEqualTo(newRemoteEventForLocalEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[noteId1]).isEqualTo(initialState.lastSynchronizedLocalRevisions[noteId1])
        assertThat(finalState.lastSynchronizedLocalRevisions[noteId2]).isEqualTo(newLocalEventForRemoteEvent2.revision)
    }

    @Test
    fun `do not synchronize events created during synchronization`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 1)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, noteId = 1, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, noteId = 1, revision = 13)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, noteId = 1, revision = 3)
        val noteId1 = compensatedLocalEvent1.noteId
        initialState = initialState
                .updateLastKnownLocalRevision(noteId1, newLocalEventForRemoteEvent1.revision - 1)
                .updateLastKnownRemoteRevision(noteId1, newRemoteEventForLocalEvent1.revision - 1)
        givenLocalEvents(compensatedLocalEvent1)
        givenRemoteEvents(compensatedRemoteEvent1)
        every {
            synchronizationStrategy.resolve(
                    noteId = noteId1,
                    localEvents = listOf(compensatedLocalEvent1),
                    remoteEvents = listOf(compensatedRemoteEvent1)
            )
        }.returns(Solution(listOf(
                CompensatingAction(
                        compensatedLocalEvents = listOf(compensatedLocalEvent1),
                        compensatedRemoteEvents = listOf(compensatedRemoteEvent1),
                        newLocalEvents = listOf(compensatingEventForRemoteEvent1),
                        newRemoteEvents = listOf(compensatingEventForLocalEvent1)
                )
        )))
        val commandForLocalEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent1,
                lastRevision = initialState.lastKnownRemoteRevisions[noteId1],
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent1
        )
        val commandForRemoteEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent1,
                lastRevision = initialState.lastKnownLocalRevisions[noteId1],
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent1
        )
        val s = createSynchronizer()
        s.synchronize()
        givenLocalEvents(newLocalEventForRemoteEvent1)
        givenRemoteEvents(newRemoteEventForLocalEvent1)

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1)
            localCommandExecutor.execute(commandForRemoteEvent1)
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            localEvents.removeEvent(compensatedLocalEvent1)
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteEvents.removeEvent(newRemoteEventForLocalEvent1)
            localEvents.removeEvent(newLocalEventForRemoteEvent1)
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
                    eventToCommandMapper,
                    localCommandExecutor,
                    remoteCommandExecutor,
                    initialState
            )

    private fun givenLocalEvents(vararg events: Event) {
        every { localEvents.getEvents() }.returns(events.toList().toObservable())
    }

    private fun givenRemoteEvents(vararg events: Event) {
        every { remoteEvents.getEvents() }.returns(events.toList().toObservable())
    }

    private fun givenTheSuccessfulExecutionOfACompensatingEvent(compensatingEvent: Event, lastRevision: Int?, commandExecutor: CommandExecutor, newEvent: Event): Command {
        val command = modelCommand(compensatingEvent.noteId, lastRevision)
        every { eventToCommandMapper.map(compensatingEvent, lastRevision) }.returns(command)
        every { commandExecutor.execute(command) }.returns(CommandExecutor.ExecutionResult.Success(
                newEventMetadata = CommandExecutor.EventMetadata(newEvent)
        ))
        return command
    }

    private fun givenTheFailedExecutionOfACompensatingEvent(compensatingEvent: Event, lastRevision: Int?, commandExecutor: CommandExecutor, newEvent: Event): Command {
        val command = modelCommand(compensatingEvent.noteId, lastRevision)
        every { eventToCommandMapper.map(compensatingEvent, lastRevision) }.returns(command)
        every { commandExecutor.execute(command) }.returns(CommandExecutor.ExecutionResult.Failure)
        return command
    }

    companion object {
        private fun assertThatTheStateDidNotChange(stateObserver: TestObserver<SynchronizerState>) {
            assertThat(stateObserver.values().toList()).isEmpty()
        }

        internal fun modelCommand(noteId: String, lastRevision: Int? = null): Command {
            return if (lastRevision == null) {
                CreateNoteCommand(noteId, "Title $noteId")
            } else {
                DeleteNoteCommand(noteId, lastRevision)
            }

        }

        internal fun modelEvent(eventId: Int, noteId: Int, revision: Int): NoteCreatedEvent {
            return NoteCreatedEvent(eventId = eventId, noteId = "note-$noteId", revision = revision, title = "Title $noteId")
        }
    }
}
