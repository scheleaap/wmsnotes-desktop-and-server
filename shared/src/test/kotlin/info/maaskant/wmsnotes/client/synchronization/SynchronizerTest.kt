package info.maaskant.wmsnotes.client.synchronization

import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import info.maaskant.wmsnotes.client.synchronization.commandexecutor.CommandExecutor
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult.Solution
import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.CommandError
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.note.CreateNoteCommand
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import io.mockk.*
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.rxkotlin.toObservable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SynchronizerTest {
    private val aggId1 = "n-10000000-0000-0000-0000-000000000000"
    private val aggId2 = "n-20000000-0000-0000-0000-000000000000"

    private val localEvents: ModifiableEventRepository = mockk(name = "local")
    private val remoteEvents: ModifiableEventRepository = mockk(name = "remote")
    private val synchronizationStrategy: SynchronizationStrategy = mockk()
    private val eventToCommandMapper: EventToCommandMapper = mockk()
    private val localCommandExecutor: CommandExecutor = mockk(name = "local")
    private val remoteCommandExecutor: CommandExecutor = mockk(name = "remote")
    private lateinit var initialState: SynchronizerState

    @BeforeEach
    fun init() {
        clearMocks(
                localEvents,
                remoteEvents,
                synchronizationStrategy,
                eventToCommandMapper,
                localCommandExecutor,
                remoteCommandExecutor
        )
        every { localCommandExecutor.execute(any(), any()) }.returns(CommandExecutor.ExecutionResult.Failure(CommandError.OtherError("Test")))
        every { remoteCommandExecutor.execute(any(), any()) }.returns(CommandExecutor.ExecutionResult.Failure(CommandError.OtherError("Test")))
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
        val result = s.synchronize()

        // Then
        assertThat(result).isEqualTo(SynchronizationResult(
                success = true
        ))
        verify {
            synchronizationStrategy.resolve(any(), any(), any()).wasNot(Called)
            localCommandExecutor.execute(any(), any()).wasNot(Called)
            remoteCommandExecutor.execute(any(), any()).wasNot(Called)
        }
    }

    @Test
    fun `nothing happens if synchronization strategy has no solution`() {
        // Given
        val localEvent: Event = modelEvent(eventId = 11, aggId = aggId1, revision = 1)
        val remoteEvent: Event = modelEvent(eventId = 1, aggId = aggId1, revision = 11)
        every { localEvents.getEvents() }.returns(Observable.just(localEvent))
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteEvent))
        every { synchronizationStrategy.resolve(any(), any(), any()) }.returns(NoSolution)
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verify {
            synchronizationStrategy.resolve(aggId1, listOf(localEvent), listOf(remoteEvent))
        }
        verify(exactly = 0) {
            localCommandExecutor.execute(any(), any())
            remoteCommandExecutor.execute(any(), any())
            localEvents.removeEvent(any())
            remoteEvents.removeEvent(any())
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[aggId1]).isEqualTo(localEvent.revision)
        assertThat(finalState.lastKnownRemoteRevisions[aggId1]).isEqualTo(remoteEvent.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[aggId1]).isNull()
    }

    @Test
    fun `one note, one compensating action, only local events`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, aggId = aggId1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, aggId = aggId1, revision = 2)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, aggId = aggId1, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, aggId = aggId1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, aggId = aggId1, revision = 14)
        initialState = initialState
                .updateLastKnownRemoteRevision(aggId1, newRemoteEventForLocalEvent1.revision - 1)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents()
        every {
            synchronizationStrategy.resolve(
                    aggId = aggId1,
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
                lastRevision = initialState.lastKnownRemoteRevisions[aggId1]!!,
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
        val result = s.synchronize()

        // Then
        assertThat(result).isEqualTo(SynchronizationResult(success = true))
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1.first, lastRevision = commandForLocalEvent1.second)
            remoteCommandExecutor.execute(commandForLocalEvent2.first, lastRevision = commandForLocalEvent2.second)
            localEvents.removeEvent(compensatedLocalEvent1)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[aggId1]).isEqualTo(compensatedLocalEvent2.revision)
        assertThat(finalState.lastKnownRemoteRevisions[aggId1]).isEqualTo(newRemoteEventForLocalEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[aggId1]).isEqualTo(compensatedLocalEvent2.revision)
    }

    @Test
    fun `one note, one compensating action, only remote events`() {
        // Given
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, aggId = aggId1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, aggId = aggId1, revision = 12)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, aggId = aggId1, revision = 0)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, aggId = aggId1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, aggId = aggId1, revision = 4)
        initialState = initialState
                .updateLastKnownLocalRevision(aggId1, newLocalEventForRemoteEvent1.revision - 1)
        every { localEvents.getEvents() }.returns(Observable.empty())
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    aggId = aggId1,
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
                lastRevision = initialState.lastKnownLocalRevisions[aggId1]!!,
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
        val result = s.synchronize()

        // Then
        assertThat(result).isEqualTo(SynchronizationResult(success = true))
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            localCommandExecutor.execute(commandForRemoteEvent1.first, lastRevision = commandForRemoteEvent1.second)
            localCommandExecutor.execute(commandForRemoteEvent2.first, lastRevision = commandForRemoteEvent2.second)
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[aggId1]).isEqualTo(newLocalEventForRemoteEvent2.revision)
        assertThat(finalState.lastKnownRemoteRevisions[aggId1]).isEqualTo(compensatedRemoteEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[aggId1]).isEqualTo(newLocalEventForRemoteEvent2.revision)
    }

    @Test
    fun `one note, one compensating action`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, aggId = aggId1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, aggId = aggId1, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, aggId = aggId1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, aggId = aggId1, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, aggId = aggId1, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, aggId = aggId1, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, aggId = aggId1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, aggId = aggId1, revision = 14)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, aggId = aggId1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, aggId = aggId1, revision = 4)
        initialState = initialState
                .updateLastKnownLocalRevision(aggId1, compensatedLocalEvent2.revision)
                .updateLastKnownRemoteRevision(aggId1, compensatedRemoteEvent2.revision)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    aggId = aggId1,
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
                lastRevision = initialState.lastKnownRemoteRevisions[aggId1]!!,
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
                lastRevision = initialState.lastKnownLocalRevisions[aggId1]!!,
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
        val result = s.synchronize()

        // Then
        assertThat(result).isEqualTo(SynchronizationResult(success = true))
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1.first, lastRevision = commandForLocalEvent1.second)
            remoteCommandExecutor.execute(commandForLocalEvent2.first, lastRevision = commandForLocalEvent2.second)
            localCommandExecutor.execute(commandForRemoteEvent1.first, lastRevision = commandForRemoteEvent1.second)
            localCommandExecutor.execute(commandForRemoteEvent2.first, lastRevision = commandForRemoteEvent2.second)
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
            localEvents.removeEvent(compensatedLocalEvent1)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[aggId1]).isEqualTo(newLocalEventForRemoteEvent2.revision)
        assertThat(finalState.lastKnownRemoteRevisions[aggId1]).isEqualTo(newRemoteEventForLocalEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[aggId1]).isEqualTo(newLocalEventForRemoteEvent2.revision)
    }

    @Test
    fun `one note, multiple compensating actions`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, aggId = aggId1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, aggId = aggId1, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, aggId = aggId1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, aggId = aggId1, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, aggId = aggId1, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, aggId = aggId1, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, aggId = aggId1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, aggId = aggId1, revision = 14)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, aggId = aggId1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, aggId = aggId1, revision = 4)
        initialState = initialState
                .updateLastKnownLocalRevision(aggId1, compensatedLocalEvent2.revision)
                .updateLastKnownRemoteRevision(aggId1, compensatedRemoteEvent2.revision)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    aggId = aggId1,
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
                lastRevision = initialState.lastKnownRemoteRevisions[aggId1]!!,
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
                lastRevision = initialState.lastKnownLocalRevisions[aggId1]!!,
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
        val result = s.synchronize()

        // Then
        assertThat(result).isEqualTo(SynchronizationResult(success = true))
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1.first, lastRevision = commandForLocalEvent1.second)
            localCommandExecutor.execute(commandForRemoteEvent1.first, lastRevision = commandForRemoteEvent1.second)
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            localEvents.removeEvent(compensatedLocalEvent1)
            remoteCommandExecutor.execute(commandForLocalEvent2.first, lastRevision = commandForLocalEvent2.second)
            localCommandExecutor.execute(commandForRemoteEvent2.first, lastRevision = commandForRemoteEvent2.second)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[aggId1]).isEqualTo(newLocalEventForRemoteEvent2.revision)
        assertThat(finalState.lastKnownRemoteRevisions[aggId1]).isEqualTo(newRemoteEventForLocalEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[aggId1]).isEqualTo(newLocalEventForRemoteEvent2.revision)
    }

    @Test
    fun `one note, multiple compensating actions, executing local command fails`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, aggId = aggId1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, aggId = aggId1, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, aggId = aggId1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, aggId = aggId1, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, aggId = aggId1, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, aggId = aggId1, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, aggId = aggId1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, aggId = aggId1, revision = 14)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, aggId = aggId1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, aggId = aggId1, revision = 4)
        initialState = initialState
                .updateLastKnownLocalRevision(aggId1, compensatedLocalEvent2.revision)
                .updateLastKnownRemoteRevision(aggId1, compensatedRemoteEvent2.revision)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    aggId = aggId1,
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
                lastRevision = initialState.lastKnownRemoteRevisions[aggId1]!!,
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
                lastRevision = initialState.lastKnownLocalRevisions[aggId1]!!,
                commandExecutor = localCommandExecutor
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
        val result = s.synchronize()

        // Then
        assertThat(result).isEqualTo(SynchronizationResult(success = false))
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1.first, lastRevision = commandForLocalEvent1.second)
            localCommandExecutor.execute(commandForRemoteEvent1.first, lastRevision = commandForRemoteEvent1.second)
        }
        verify(exactly = 0) {
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            localEvents.removeEvent(compensatedLocalEvent1)
            remoteCommandExecutor.execute(commandForLocalEvent2.first, lastRevision = commandForLocalEvent2.second)
            localCommandExecutor.execute(commandForRemoteEvent2.first, lastRevision = commandForRemoteEvent2.second)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[aggId1]).isEqualTo(initialState.lastKnownLocalRevisions[aggId1])
        assertThat(finalState.lastKnownRemoteRevisions[aggId1]).isEqualTo(newRemoteEventForLocalEvent1.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[aggId1]).isEqualTo(initialState.lastSynchronizedLocalRevisions[aggId1])
    }

    @Test
    fun `one note, multiple compensating actions, executing remote command fails`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, aggId = aggId1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, aggId = aggId1, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, aggId = aggId1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, aggId = aggId1, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, aggId = aggId1, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, aggId = aggId1, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, aggId = aggId1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, aggId = aggId1, revision = 14)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, aggId = aggId1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, aggId = aggId1, revision = 4)
        initialState = initialState
                .updateLastKnownLocalRevision(aggId1, compensatedLocalEvent2.revision)
                .updateLastKnownRemoteRevision(aggId1, compensatedRemoteEvent2.revision)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    aggId = aggId1,
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
                lastRevision = initialState.lastKnownRemoteRevisions[aggId1]!!,
                commandExecutor = remoteCommandExecutor
        )
        val commandForLocalEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent2,
                lastRevision = newRemoteEventForLocalEvent1.revision,
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent2
        )
        val commandForRemoteEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent1,
                lastRevision = initialState.lastKnownLocalRevisions[aggId1]!!,
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
        val result = s.synchronize()

        // Then
        assertThat(result).isEqualTo(SynchronizationResult(success = false))
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1.first, lastRevision = commandForLocalEvent1.second)
        }
        verify(exactly = 0) {
            localCommandExecutor.execute(commandForRemoteEvent1.first, lastRevision = commandForRemoteEvent1.second)
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            localEvents.removeEvent(compensatedLocalEvent1)
            remoteCommandExecutor.execute(commandForLocalEvent2.first, lastRevision = commandForLocalEvent2.second)
            localCommandExecutor.execute(commandForRemoteEvent2.first, lastRevision = commandForRemoteEvent2.second)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        assertThatTheStateDidNotChange(stateObserver)
    }

    @Test
    fun `one note, multiple compensating actions, different events in compensating actions`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, aggId = aggId1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, aggId = aggId1, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, aggId = aggId1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, aggId = aggId1, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, aggId = aggId1, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, aggId = aggId1, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, aggId = aggId1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, aggId = aggId1, revision = 14)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, aggId = aggId1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, aggId = aggId1, revision = 4)
        initialState = initialState
                .updateLastKnownLocalRevision(aggId1, compensatedLocalEvent2.revision)
                .updateLastKnownRemoteRevision(aggId1, compensatedRemoteEvent2.revision)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    aggId = aggId1,
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
                lastRevision = initialState.lastKnownRemoteRevisions[aggId1]!!,
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
                lastRevision = initialState.lastKnownLocalRevisions[aggId1]!!,
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
            localCommandExecutor.execute(any(), any())
            remoteCommandExecutor.execute(any(), any())
            localEvents.removeEvent(any())
            remoteEvents.removeEvent(any())
        }
        assertThatTheStateDidNotChange(stateObserver)
    }

    @Test
    fun `multiple notes, multiple compensating actions`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, aggId = aggId1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, aggId = aggId2, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, aggId = aggId1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, aggId = aggId2, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, aggId = aggId2, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, aggId = aggId2, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, aggId = aggId1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, aggId = aggId2, revision = 14)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, aggId = aggId1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, aggId = aggId2, revision = 4)
        initialState = initialState
                .updateLastKnownLocalRevision(aggId1, compensatedLocalEvent1.revision)
                .updateLastKnownLocalRevision(aggId2, compensatedLocalEvent2.revision)
                .updateLastKnownRemoteRevision(aggId1, compensatedRemoteEvent1.revision)
                .updateLastKnownRemoteRevision(aggId2, compensatedRemoteEvent2.revision)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    aggId = aggId1,
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
                    aggId = aggId2,
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
                lastRevision = initialState.lastKnownRemoteRevisions[aggId1]!!,
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent1
        )
        val commandForLocalEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent2,
                lastRevision = initialState.lastKnownRemoteRevisions[aggId2]!!,
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent2
        )
        val commandForRemoteEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent1,
                lastRevision = initialState.lastKnownLocalRevisions[aggId1]!!,
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent1
        )
        val commandForRemoteEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent2,
                lastRevision = initialState.lastKnownLocalRevisions[aggId2]!!,
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent2
        )
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        val result = s.synchronize()

        // Then
        assertThat(result).isEqualTo(SynchronizationResult(success = true))
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1.first, lastRevision = commandForLocalEvent1.second)
            localCommandExecutor.execute(commandForRemoteEvent1.first, lastRevision = commandForRemoteEvent1.second)
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            localEvents.removeEvent(compensatedLocalEvent1)
            remoteCommandExecutor.execute(commandForLocalEvent2.first, lastRevision = commandForLocalEvent2.second)
            localCommandExecutor.execute(commandForRemoteEvent2.first, lastRevision = commandForRemoteEvent2.second)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[aggId1]).isEqualTo(newLocalEventForRemoteEvent1.revision)
        assertThat(finalState.lastKnownLocalRevisions[aggId2]).isEqualTo(newLocalEventForRemoteEvent2.revision)
        assertThat(finalState.lastKnownRemoteRevisions[aggId1]).isEqualTo(newRemoteEventForLocalEvent1.revision)
        assertThat(finalState.lastKnownRemoteRevisions[aggId2]).isEqualTo(newRemoteEventForLocalEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[aggId1]).isEqualTo(newLocalEventForRemoteEvent1.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[aggId2]).isEqualTo(newLocalEventForRemoteEvent2.revision)
    }

    @Test
    fun `multiple notes, multiple compensating actions, executing local command fails`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, aggId = aggId1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, aggId = aggId2, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, aggId = aggId1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, aggId = aggId2, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, aggId = aggId2, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, aggId = aggId2, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, aggId = aggId1, revision = 13)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, aggId = aggId2, revision = 14)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, aggId = aggId2, revision = 4)
        initialState = initialState
                .updateLastKnownLocalRevision(aggId1, compensatedLocalEvent1.revision)
                .updateLastKnownLocalRevision(aggId2, compensatedLocalEvent2.revision)
                .updateLastKnownRemoteRevision(aggId1, compensatedRemoteEvent1.revision)
                .updateLastKnownRemoteRevision(aggId2, compensatedRemoteEvent2.revision)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    aggId = aggId1,
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
                    aggId = aggId2,
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
                lastRevision = initialState.lastKnownRemoteRevisions[aggId1]!!,
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent1
        )
        val commandForLocalEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent2,
                lastRevision = initialState.lastKnownRemoteRevisions[aggId2]!!,
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent2
        )
        val commandForRemoteEvent1 = givenTheFailedExecutionOfACompensatingEvent( // Failure
                compensatingEvent = compensatingEventForRemoteEvent1,
                lastRevision = initialState.lastKnownLocalRevisions[aggId1]!!,
                commandExecutor = localCommandExecutor
        )
        val commandForRemoteEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent2,
                lastRevision = initialState.lastKnownLocalRevisions[aggId2]!!,
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent2
        )
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        val result = s.synchronize()

        // Then
        assertThat(result).isEqualTo(SynchronizationResult(success = false))
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1.first, lastRevision = commandForLocalEvent1.second)
            localCommandExecutor.execute(commandForRemoteEvent1.first, lastRevision = commandForRemoteEvent1.second)
            remoteCommandExecutor.execute(commandForLocalEvent2.first, lastRevision = commandForLocalEvent2.second)
            localCommandExecutor.execute(commandForRemoteEvent2.first, lastRevision = commandForRemoteEvent2.second)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        verify(exactly = 0) {
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            localEvents.removeEvent(compensatedLocalEvent1)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[aggId1]).isEqualTo(initialState.lastKnownLocalRevisions[aggId1])
        assertThat(finalState.lastKnownLocalRevisions[aggId2]).isEqualTo(newLocalEventForRemoteEvent2.revision)
        assertThat(finalState.lastKnownRemoteRevisions[aggId1]).isEqualTo(newRemoteEventForLocalEvent1.revision)
        assertThat(finalState.lastKnownRemoteRevisions[aggId2]).isEqualTo(newRemoteEventForLocalEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[aggId1]).isEqualTo(initialState.lastSynchronizedLocalRevisions[aggId1])
        assertThat(finalState.lastSynchronizedLocalRevisions[aggId2]).isEqualTo(newLocalEventForRemoteEvent2.revision)
    }

    @Test
    fun `multiple notes, multiple compensating actions, executing remote command fails`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, aggId = aggId1, revision = 1)
        val compensatedLocalEvent2 = modelEvent(eventId = 12, aggId = aggId2, revision = 2)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, aggId = aggId1, revision = 11)
        val compensatedRemoteEvent2 = modelEvent(eventId = 2, aggId = aggId2, revision = 12)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForLocalEvent2 = modelEvent(eventId = -2, aggId = aggId2, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForRemoteEvent2 = modelEvent(eventId = -2, aggId = aggId2, revision = 0)
        val newRemoteEventForLocalEvent2 = modelEvent(eventId = 4, aggId = aggId2, revision = 14)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, aggId = aggId1, revision = 3)
        val newLocalEventForRemoteEvent2 = modelEvent(eventId = 14, aggId = aggId2, revision = 4)
        initialState = initialState
                .updateLastKnownLocalRevision(aggId1, compensatedLocalEvent1.revision)
                .updateLastKnownLocalRevision(aggId2, compensatedLocalEvent2.revision)
                .updateLastKnownRemoteRevision(aggId1, compensatedRemoteEvent1.revision)
                .updateLastKnownRemoteRevision(aggId2, compensatedRemoteEvent2.revision)
        givenLocalEvents(compensatedLocalEvent1, compensatedLocalEvent2)
        givenRemoteEvents(compensatedRemoteEvent1, compensatedRemoteEvent2)
        every {
            synchronizationStrategy.resolve(
                    aggId = aggId1,
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
                    aggId = aggId2,
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
                lastRevision = initialState.lastKnownRemoteRevisions[aggId1]!!,
                commandExecutor = remoteCommandExecutor
        )
        val commandForLocalEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForLocalEvent2,
                lastRevision = initialState.lastKnownRemoteRevisions[aggId2]!!,
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent2
        )
        val commandForRemoteEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent1,
                lastRevision = initialState.lastKnownLocalRevisions[aggId1]!!,
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent1
        )
        val commandForRemoteEvent2 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent2,
                lastRevision = initialState.lastKnownLocalRevisions[aggId2]!!,
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent2
        )
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        val result = s.synchronize()

        // Then
        assertThat(result).isEqualTo(SynchronizationResult(success = false))
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1.first, lastRevision = commandForLocalEvent1.second)
            remoteCommandExecutor.execute(commandForLocalEvent2.first, lastRevision = commandForLocalEvent2.second)
            localCommandExecutor.execute(commandForRemoteEvent2.first, lastRevision = commandForRemoteEvent2.second)
            remoteEvents.removeEvent(compensatedRemoteEvent2)
            localEvents.removeEvent(compensatedLocalEvent2)
        }
        verify(exactly = 0) {
            localCommandExecutor.execute(commandForRemoteEvent1.first, lastRevision = commandForRemoteEvent1.second)
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            localEvents.removeEvent(compensatedLocalEvent1)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.lastKnownLocalRevisions[aggId1]).isEqualTo(initialState.lastKnownLocalRevisions[aggId1])
        assertThat(finalState.lastKnownLocalRevisions[aggId2]).isEqualTo(newLocalEventForRemoteEvent2.revision)
        assertThat(finalState.lastKnownRemoteRevisions[aggId1]).isEqualTo(initialState.lastKnownRemoteRevisions[aggId1])
        assertThat(finalState.lastKnownRemoteRevisions[aggId2]).isEqualTo(newRemoteEventForLocalEvent2.revision)
        assertThat(finalState.lastSynchronizedLocalRevisions[aggId1]).isEqualTo(initialState.lastSynchronizedLocalRevisions[aggId1])
        assertThat(finalState.lastSynchronizedLocalRevisions[aggId2]).isEqualTo(newLocalEventForRemoteEvent2.revision)
    }

    @Test
    fun `do not synchronize events created during synchronization`() {
        // Given
        val compensatedLocalEvent1 = modelEvent(eventId = 11, aggId = aggId1, revision = 1)
        val compensatedRemoteEvent1 = modelEvent(eventId = 1, aggId = aggId1, revision = 11)
        val compensatingEventForLocalEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val compensatingEventForRemoteEvent1 = modelEvent(eventId = -1, aggId = aggId1, revision = 0)
        val newRemoteEventForLocalEvent1 = modelEvent(eventId = 3, aggId = aggId1, revision = 13)
        val newLocalEventForRemoteEvent1 = modelEvent(eventId = 13, aggId = aggId1, revision = 3)
        val aggId1 = compensatedLocalEvent1.aggId
        initialState = initialState
                .updateLastKnownLocalRevision(aggId1, compensatedLocalEvent1.revision)
                .updateLastKnownRemoteRevision(aggId1, compensatedRemoteEvent1.revision)
        givenLocalEvents(compensatedLocalEvent1)
        givenRemoteEvents(compensatedRemoteEvent1)
        every {
            synchronizationStrategy.resolve(
                    aggId = aggId1,
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
                lastRevision = initialState.lastKnownRemoteRevisions[aggId1]!!,
                commandExecutor = remoteCommandExecutor,
                newEvent = newRemoteEventForLocalEvent1
        )
        val commandForRemoteEvent1 = givenTheSuccessfulExecutionOfACompensatingEvent(
                compensatingEvent = compensatingEventForRemoteEvent1,
                lastRevision = initialState.lastKnownLocalRevisions[aggId1]!!,
                commandExecutor = localCommandExecutor,
                newEvent = newLocalEventForRemoteEvent1
        )
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()
        s.synchronize()
        givenLocalEvents(newLocalEventForRemoteEvent1)
        givenRemoteEvents(newRemoteEventForLocalEvent1)

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteCommandExecutor.execute(commandForLocalEvent1.first, lastRevision = commandForLocalEvent1.second)
            localCommandExecutor.execute(commandForRemoteEvent1.first, lastRevision = commandForRemoteEvent1.second)
            remoteEvents.removeEvent(compensatedRemoteEvent1)
            localEvents.removeEvent(compensatedLocalEvent1)
            localEvents.getEvents()
            remoteEvents.getEvents()
            remoteEvents.removeEvent(newRemoteEventForLocalEvent1)
            localEvents.removeEvent(newLocalEventForRemoteEvent1)
        }
        val finalState = stateObserver.values().last()
        assertThat(finalState.localEventIdsToIgnore).doesNotContain(newLocalEventForRemoteEvent1.eventId)
        assertThat(finalState.remoteEventIdsToIgnore).doesNotContain(newRemoteEventForLocalEvent1.eventId)
    }

    private fun createSynchronizer(
            localEvents: ModifiableEventRepository = this.localEvents,
            remoteEvents: ModifiableEventRepository = this.remoteEvents
    ) =
            Synchronizer(
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

    private fun givenTheSuccessfulExecutionOfACompensatingEvent(compensatingEvent: Event, lastRevision: Int, commandExecutor: CommandExecutor, newEvent: Event): Pair<Command, Int> {
        val command = modelCommand(compensatingEvent.aggId)
        every { eventToCommandMapper.map(compensatingEvent) }.returns(command)
        every { commandExecutor.execute(command, lastRevision) }.returns(CommandExecutor.ExecutionResult.Success(
                newEventMetadata = CommandExecutor.EventMetadata(newEvent)
        ))
        return command to lastRevision
    }

    private fun givenTheFailedExecutionOfACompensatingEvent(compensatingEvent: Event, lastRevision: Int, commandExecutor: CommandExecutor): Pair<Command, Int> {
        val command = modelCommand(compensatingEvent.aggId)
        every { eventToCommandMapper.map(compensatingEvent) }.returns(command)
        every { commandExecutor.execute(command, lastRevision) }.returns(CommandExecutor.ExecutionResult.Failure(CommandError.OtherError("Test")))
        return command to lastRevision
    }

    companion object {
        private fun assertThatTheStateDidNotChange(stateObserver: TestObserver<SynchronizerState>) {
            assertThat(stateObserver.values().toList()).isEmpty()
        }

        internal fun modelCommand(aggId: String): Command {
            return CreateNoteCommand(aggId, path = Path("path-$aggId"), title = "Title $aggId", content = "Text $aggId")
        }

        internal fun modelEvent(eventId: Int, aggId: String, revision: Int): NoteCreatedEvent {
            return NoteCreatedEvent(eventId = eventId, aggId = aggId, revision = revision, path = Path("path-$aggId"), title = "Title $aggId", content = "Text $aggId")
        }
    }
}
