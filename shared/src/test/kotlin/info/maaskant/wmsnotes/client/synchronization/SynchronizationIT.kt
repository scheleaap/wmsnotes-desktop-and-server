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
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.InMemoryEventStore
import info.maaskant.wmsnotes.model.projection.cache.CachingNoteProjector
import info.maaskant.wmsnotes.model.projection.cache.NoopNoteCache
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SynchronizationIT {
    private val noteId = "note"
    private val newNoteId = "new-note"

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
                                noteIdGenerator = { newNoteId }
                        ),
                        noteProjector = CachingNoteProjector(
                                eventStore = eventStore,
                                noteCache = NoopNoteCache
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
        val oldLocalEvent = NoteCreatedEvent(eventId = 0, noteId = noteId, revision = 1, title = "Note")
        val compensatedLocalEvent1 = ContentChangedEvent(eventId = 0, noteId = noteId, revision = 2, content = "Text 1")
        val compensatedRemoteEvent1 = ContentChangedEvent(eventId = 0, noteId = noteId, revision = 5 /* The remote revision can be different from the local one, for example due to previous conflicts */, content = "Text 2")
        val localChangeCommand = ChangeContentCommand(noteId = noteId, lastRevision = 2, content = "Text 2")
        val localChangeEvent = CommandExecutor.EventMetadata(eventId = 0, noteId = noteId, revision = 3)
        val newNoteCommand1 = CreateNoteCommand(noteId = newNoteId, title = newNoteId)
        val newNoteCommand2 = ChangeTitleCommand(noteId = newNoteId, lastRevision = 1, title = "Note")
        val newNoteCommand3 = ChangeContentCommand(noteId = newNoteId, lastRevision = 2, content = "Text 1")
        val newNoteEvent1 = CommandExecutor.EventMetadata(eventId = 0, noteId = newNoteId, revision = 1)
        val newNoteEvent2 = CommandExecutor.EventMetadata(eventId = 0, noteId = newNoteId, revision = 2)
        val newNoteEvent3 = CommandExecutor.EventMetadata(eventId = 0, noteId = newNoteId, revision = 3)
        val noteId = compensatedLocalEvent1.noteId
        initialState = initialState
                .updateLastKnownLocalRevision(noteId, oldLocalEvent.revision)
                .updateLastKnownRemoteRevision(noteId, oldLocalEvent.revision)
        givenOldLocalEvents(oldLocalEvent)
        givenNewLocalEvents(compensatedLocalEvent1)
        givenRemoteEvents(compensatedRemoteEvent1)
        givenALocalCommandCanBeExecutedSuccessfully(localChangeCommand, localChangeEvent)
        givenALocalCommandCanBeExecutedSuccessfully(newNoteCommand1, newNoteEvent1)
        givenALocalCommandCanBeExecutedSuccessfully(newNoteCommand2, newNoteEvent2)
        givenALocalCommandCanBeExecutedSuccessfully(newNoteCommand3, newNoteEvent3)
        givenARemoteCommandCanBeExecutedSuccessfully(newNoteCommand1, newNoteEvent1)
        givenARemoteCommandCanBeExecutedSuccessfully(newNoteCommand2, newNoteEvent2)
        givenARemoteCommandCanBeExecutedSuccessfully(newNoteCommand3, newNoteEvent3)
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verifySequence {
            remoteCommandExecutor.execute(newNoteCommand1)
            remoteCommandExecutor.execute(newNoteCommand2)
            remoteCommandExecutor.execute(newNoteCommand3)
            localCommandExecutor.execute(localChangeCommand)
            localCommandExecutor.execute(newNoteCommand1)
            localCommandExecutor.execute(newNoteCommand2)
            localCommandExecutor.execute(newNoteCommand3)
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
            NewSynchronizer(
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


    companion object {
        internal fun modelEvent(eventId: Int, noteId: Int, revision: Int): NoteCreatedEvent {
            return NoteCreatedEvent(eventId = eventId, noteId = "note-$noteId", revision = revision, title = "Title $noteId")
        }
    }
}
