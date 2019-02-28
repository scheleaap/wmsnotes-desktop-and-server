package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.synchronization.commandexecutor.CommandExecutor
import info.maaskant.wmsnotes.client.synchronization.eventrepository.InMemoryModifiableEventRepository
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.client.synchronization.strategy.LocalOnlySynchronizationStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.MultipleSynchronizationStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.RemoteOnlySynchronizationStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.DifferenceAnalyzer
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.DifferenceCompensator
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.KeepBothMergeStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergingSynchronizationStrategy
import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.InMemoryEventStore
import info.maaskant.wmsnotes.model.note.*
import info.maaskant.wmsnotes.model.aggregaterepository.CachingAggregateRepository
import info.maaskant.wmsnotes.model.aggregaterepository.NoopAggregateCache
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SynchronizationIT {
    private val aggId = "note"
    private val newAggId = "new-note"

    private lateinit var eventStore: EventStore
    private lateinit var localEvents: ModifiableEventRepository
    private lateinit var remoteEvents: ModifiableEventRepository
    private lateinit var synchronizationStrategy: SynchronizationStrategy
    private val eventToCommandMapper: EventToCommandMapper = EventToCommandMapper()
    private val localCommandExecutor: CommandExecutor = mockk()
    private val remoteCommandExecutor: CommandExecutor = mockk()
    private lateinit var initialState: SynchronizerState

    @BeforeEach
    fun init() {
        eventStore = InMemoryEventStore()
        localEvents = InMemoryModifiableEventRepository()
        remoteEvents = InMemoryModifiableEventRepository()
        synchronizationStrategy = MultipleSynchronizationStrategy(
                LocalOnlySynchronizationStrategy(),
                RemoteOnlySynchronizationStrategy(),
                MergingSynchronizationStrategy(
                        mergeStrategy = KeepBothMergeStrategy(
                                differenceAnalyzer = DifferenceAnalyzer(),
                                differenceCompensator = DifferenceCompensator(),
                                aggregateIdGenerator = { newAggId }
                        ),
                        noteRepository = CachingAggregateRepository(
                                eventStore = eventStore,
                                aggregateCache = NoopAggregateCache(),
                                emptyAggregate = Note()
                        )
                )
        )
        clearMocks(
                localCommandExecutor,
                remoteCommandExecutor
        )
        every { localCommandExecutor.execute(any()) }.returns(CommandExecutor.ExecutionResult.Success(
                newEventMetadata = null
        ))
        every { remoteCommandExecutor.execute(any()) }.returns(CommandExecutor.ExecutionResult.Success(
                newEventMetadata = null
        ))
        initialState = SynchronizerState.create()
    }

    @Test
    fun `scenario 1`() {
        // Given
        val oldLocalEvent = NoteCreatedEvent(eventId = 0, aggId = aggId, revision = 1, path = Path("path"), title = "Note", content = "Some old text")
        val compensatedLocalEvent1 = ContentChangedEvent(eventId = 0, aggId = aggId, revision = 2, content = "Text 1")
        val compensatedRemoteEvent1 = ContentChangedEvent(eventId = 0, aggId = aggId, revision = 5 /* The remote revision can be different from the local one, for example due to previous conflicts */, content = "Text 2")
        val localChangeCommand = ChangeContentCommand(aggId = aggId, lastRevision = 2, content = "Text 2")
        val localChangeEvent = CommandExecutor.EventMetadata(eventId = 0, aggId = aggId, revision = 3)
        val newNoteCommand1 = CreateNoteCommand(aggId = newAggId, path = Path(), title = newAggId, content = "")
        val newNoteCommand2 = MoveCommand(aggId = newAggId, lastRevision = 1, path = Path("path"))
        val newNoteCommand3 = ChangeTitleCommand(aggId = newAggId, lastRevision = 2, title = "Note")
        val newNoteCommand4 = ChangeContentCommand(aggId = newAggId, lastRevision = 3, content = "Text 1")
        val newNoteEvent1 = CommandExecutor.EventMetadata(eventId = 0, aggId = newAggId, revision = 1)
        val newNoteEvent2 = CommandExecutor.EventMetadata(eventId = 0, aggId = newAggId, revision = 2)
        val newNoteEvent3 = CommandExecutor.EventMetadata(eventId = 0, aggId = newAggId, revision = 3)
        val newNoteEvent4 = CommandExecutor.EventMetadata(eventId = 0, aggId = newAggId, revision = 4)
        val aggId = compensatedLocalEvent1.aggId
        initialState = initialState
                .updateLastKnownLocalRevision(aggId, oldLocalEvent.revision)
                .updateLastKnownRemoteRevision(aggId, oldLocalEvent.revision)
        givenOldLocalEvents(oldLocalEvent)
        givenNewLocalEvents(compensatedLocalEvent1)
        givenRemoteEvents(compensatedRemoteEvent1)
        givenALocalCommandCanBeExecutedSuccessfully(localChangeCommand, localChangeEvent)
        givenALocalCommandCanBeExecutedSuccessfully(newNoteCommand1, newNoteEvent1)
        givenALocalCommandCanBeExecutedSuccessfully(newNoteCommand2, newNoteEvent2)
        givenALocalCommandCanBeExecutedSuccessfully(newNoteCommand3, newNoteEvent3)
        givenALocalCommandCanBeExecutedSuccessfully(newNoteCommand4, newNoteEvent4)
        givenARemoteCommandCanBeExecutedSuccessfully(newNoteCommand1, newNoteEvent1)
        givenARemoteCommandCanBeExecutedSuccessfully(newNoteCommand2, newNoteEvent2)
        givenARemoteCommandCanBeExecutedSuccessfully(newNoteCommand3, newNoteEvent3)
        givenARemoteCommandCanBeExecutedSuccessfully(newNoteCommand4, newNoteEvent4)
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verifySequence {
            remoteCommandExecutor.execute(newNoteCommand1)
            remoteCommandExecutor.execute(newNoteCommand2)
            remoteCommandExecutor.execute(newNoteCommand3)
            remoteCommandExecutor.execute(newNoteCommand4)
            localCommandExecutor.execute(localChangeCommand)
            localCommandExecutor.execute(newNoteCommand1)
            localCommandExecutor.execute(newNoteCommand2)
            localCommandExecutor.execute(newNoteCommand3)
            localCommandExecutor.execute(newNoteCommand4)
        }
    }

    private fun givenALocalCommandCanBeExecutedSuccessfully(command: Command, newEventMetadata: CommandExecutor.EventMetadata) {
        every { localCommandExecutor.execute(command) }.returns(CommandExecutor.ExecutionResult.Success(
                newEventMetadata = newEventMetadata
        ))
    }

    private fun givenARemoteCommandCanBeExecutedSuccessfully(command: Command, newEventMetadata: CommandExecutor.EventMetadata) {
        every { remoteCommandExecutor.execute(command) }.returns(CommandExecutor.ExecutionResult.Success(
                newEventMetadata = newEventMetadata
        ))
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

    private fun givenOldLocalEvents(vararg events: Event) {
        events.forEach { eventStore.appendEvent(it) }
    }

    private fun givenNewLocalEvents(vararg events: Event) {
        events.forEach {
            eventStore.appendEvent(it)
            localEvents.addEvent(it)
        }
    }

    private fun givenRemoteEvents(vararg events: Event) {
        events.forEach { remoteEvents.addEvent(it) }
    }
}
